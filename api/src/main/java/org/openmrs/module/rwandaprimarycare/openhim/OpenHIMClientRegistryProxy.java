package org.openmrs.module.rwandaprimarycare.openhim;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandaprimarycare.PrimaryCareConstants;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * OpenHIM Client Registry Proxy Service
 *
 * HTTP proxy that forwards FHIR Patient resources from Client Registry module
 * to OpenHIM for enterprise health information exchange.
 *
 * Performs lightweight transformation for OpenHIM protocol requirements:
 * - Sets Patient.id to UPI identifier value (required by OpenHIM)
 * - Validates UPI identifier exists before forwarding
 * - Passes through all other requests unchanged
 *
 * @see /docs/OPENHIM_CLIENT_REGISTRY_PROXY.md for complete documentation
 */
@Service
public class OpenHIMClientRegistryProxy {

	protected final Log log = LogFactory.getLog(getClass());

	/**
	 * FHIR Context for parsing and serializing Patient resources
	 * Initialized lazily to avoid startup overhead
	 */
	private volatile FhirContext fhirContext;

	/**
	 * HTTP headers to copy from original request to OpenHIM request
	 *
	 * NOTE: Accept-Encoding removed - prevents GZIP issues
	 * OpenHIM may return GZIP data if we forward Accept-Encoding: gzip,
	 * but Spring RestTemplate doesn't auto-decompress by default.
	 * Requesting uncompressed data ensures HAPI FHIR client can parse responses.
	 */
	private static final List<String> HEADERS_TO_COPY = Arrays.asList(
		"Content-Type",
		"Accept",
		"Accept-Language",
		"Cache-Control",
		"If-None-Match",
		"If-Modified-Since"
	);

	/**
	 * Forward FHIR request to OpenHIM Client Registry channel
	 *
	 * Transparent pass-through:
	 * 1. Extracts OpenHIM configuration from global properties
	 * 2. Builds target URL preserving path and query parameters
	 * 3. Copies relevant headers from original request
	 * 4. Adds OpenHIM Basic Authentication
	 * 5. Forwards request to OpenHIM
	 * 6. Returns OpenHIM response as-is
	 *
	 * @param httpMethod HTTP method (GET, POST, PUT, DELETE)
	 * @param resourcePath FHIR resource path (e.g., "/Patient" or "/Patient/123")
	 * @param queryString Query parameters (e.g., "family=Man&given=John")
	 * @param requestBody Request body (for POST/PUT)
	 * @param requestHeaders Original request headers
	 * @return OpenHIM response (forwarded as-is)
	 */
	public ResponseEntity<String> forwardToOpenHIM(
			String httpMethod,
			String resourcePath,
			String queryString,
			String requestBody,
			Map<String, String> requestHeaders) {

		try {
			// 1. Get OpenHIM configuration from global properties
			String openhimBaseUrl = Context.getAdministrationService()
				.getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_CR_BASE_URL);

			if (openhimBaseUrl == null || openhimBaseUrl.isEmpty()) {
				log.error("OpenHIM Client Registry base URL not configured");
				return createErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
					"OpenHIM Client Registry not configured. Set global property: " +
					PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_CR_BASE_URL);
			}

			String username = Context.getAdministrationService()
				.getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_USER_NAME);
			String password = Context.getAdministrationService()
				.getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_USER_PWD);

			if (username == null || password == null) {
				log.error("OpenHIM credentials not configured");
				return createErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
					"OpenHIM credentials not configured");
			}

			// 2. Build target URI (preserve URL encoding of query parameters)
			// Using UriComponentsBuilder with build(true) to prevent double-encoding
			// The query string from the original request is already URL-encoded
			UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(openhimBaseUrl + resourcePath);
			if (queryString != null && !queryString.isEmpty()) {
				// Use query() method which accepts pre-encoded query string
				uriBuilder.query(queryString);
			}
			// build(true) tells Spring the components are already encoded - don't encode again
			// This prevents pipe '|' from being decoded and corrupted during URL reconstruction
			URI targetUri = uriBuilder.build(true).toUri();

			log.info("Forwarding " + httpMethod + " request to OpenHIM: " + targetUri);

			// 3. Prepare headers
			HttpHeaders headers = new HttpHeaders();

			// Copy relevant headers from original request
			for (String headerName : HEADERS_TO_COPY) {
				String headerValue = requestHeaders.get(headerName);
				if (headerValue != null) {
					// Special handling for Content-Type to prevent RestTemplate from overriding it
					if ("Content-Type".equalsIgnoreCase(headerName)) {
						try {
							headers.setContentType(MediaType.parseMediaType(headerValue));
						} catch (Exception e) {
							log.warn("Failed to parse Content-Type header: " + headerValue, e);
							headers.set(headerName, headerValue);
						}
					} else {
						headers.set(headerName, headerValue);
					}
				}
			}

			// Add OpenHIM Basic Authentication (replaces any existing Authorization)
			String authHeader = createBasicAuthHeader(username, password);
			headers.set("Authorization", authHeader);

			// Ensure we accept FHIR JSON if not specified
			if (!headers.containsKey("Accept")) {
				headers.set("Accept", "application/fhir+json, application/json");
			}

			// 4. Transform Patient resource for OpenHIM (sets Patient.id = UPI value)
			String transformedBody = transformPatientForOpenHIM(requestBody, httpMethod, resourcePath);

			// 5. Create request entity
			// Use byte[] instead of String to prevent RestTemplate's StringHttpMessageConverter
			// from overriding Content-Type header to text/plain
			HttpEntity<byte[]> entity;
			if (transformedBody != null && !transformedBody.isEmpty()) {
				try {
					byte[] bodyBytes = transformedBody.getBytes("UTF-8");
					entity = new HttpEntity<byte[]>(bodyBytes, headers);
				} catch (UnsupportedEncodingException e) {
					// UTF-8 is always supported per Java spec, this should never happen
					log.error("Failed to convert request body to UTF-8 bytes (should never occur)", e);
					// Fallback with explicit UTF-8 to prevent platform-dependent encoding
					try {
						entity = new HttpEntity<byte[]>(transformedBody.getBytes("UTF-8"), headers);
					} catch (UnsupportedEncodingException uee) {
						// Last resort - use platform default (should never reach here)
						entity = new HttpEntity<byte[]>(transformedBody.getBytes(), headers);
					}
				}
			} else {
				entity = new HttpEntity<byte[]>(headers);
			}

			// 6. Forward to OpenHIM
			RestTemplate restTemplate = new RestTemplate();
			HttpMethod method = HttpMethod.valueOf(httpMethod.toUpperCase());

			// Use URI object instead of String to preserve URL encoding
			ResponseEntity<String> response = restTemplate.exchange(
				targetUri, method, entity, String.class);

			log.info("OpenHIM response: " + response.getStatusCode());

			// 6.5. Apply auto-detection fallback for Patient identifier searches
			// For GET Patient searches with identifier parameter, automatically try
			// value-only format if standard FHIR format (system|value) returns empty
			if ("GET".equalsIgnoreCase(httpMethod) &&
				resourcePath != null && resourcePath.contains("Patient") &&
				queryString != null && queryString.contains("identifier=")) {

				response = applyIdentifierSearchFallback(
					response, targetUri, queryString, entity, restTemplate, method);
			}

			// 7. Return response, removing Content-Encoding header
			// RestTemplate automatically decompresses GZIP responses, but preserves the Content-Encoding header.
			// This causes HAPI FHIR client to attempt decompression again, resulting in "Not in GZIP format" error.
			// Solution: Remove Content-Encoding header since body is already decompressed by RestTemplate.
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.putAll(response.getHeaders());
			responseHeaders.remove(HttpHeaders.CONTENT_ENCODING);

			return ResponseEntity.status(response.getStatusCode())
				.headers(responseHeaders)
				.body(response.getBody());

		} catch (HttpClientErrorException e) {
			// 4xx errors from OpenHIM (bad request, not found, etc.)
			// Forward OpenHIM's error response to client (with GZIP decompression if needed)
			String errorBody = getDecompressedErrorBody(e);
			log.error("OpenHIM client error: " + e.getStatusCode() + " - " + errorBody);
			return ResponseEntity.status(e.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorBody);

		} catch (HttpServerErrorException e) {
			// 5xx errors from OpenHIM (server error)
			// Forward OpenHIM's error response to client (with GZIP decompression if needed)
			String errorBody = getDecompressedErrorBody(e);
			log.error("OpenHIM server error: " + e.getStatusCode() + " - " + errorBody);
			return ResponseEntity.status(e.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorBody);

		} catch (ResourceAccessException e) {
			// Network errors (OpenHIM unreachable, timeout, etc.)
			log.error("Cannot reach OpenHIM: " + e.getMessage(), e);
			return createErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
				"OpenHIM Client Registry unreachable: " + e.getMessage());

		} catch (Exception e) {
			// Unexpected errors
			log.error("Unexpected error forwarding to OpenHIM", e);
			return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				"Unexpected error: " + e.getMessage());
		}
	}

	/**
	 * Transform Patient resource for OpenHIM protocol requirements
	 *
	 * OpenHIM requires Patient.id to match the UPI identifier value.
	 * This method extracts the UPI identifier and sets it as the Patient resource ID.
	 *
	 * Only transforms POST/PUT requests for Patient resources.
	 * Other requests and non-Patient resources are passed through unchanged.
	 *
	 * @param requestBody FHIR JSON request body
	 * @param httpMethod HTTP method (POST, PUT, GET, DELETE)
	 * @param resourcePath Resource path (e.g., "/Patient" or "/Patient/123")
	 * @return Transformed request body (or original if transformation not needed/failed)
	 */
	private String transformPatientForOpenHIM(String requestBody, String httpMethod, String resourcePath) {
		// Only transform POST/PUT Patient resources
		if (requestBody == null || requestBody.isEmpty()) {
			return requestBody;
		}

		if (!("POST".equalsIgnoreCase(httpMethod) || "PUT".equalsIgnoreCase(httpMethod))) {
			log.debug("Skipping transformation for " + httpMethod + " request (only POST/PUT require transformation)");
			return requestBody;
		}

		if (resourcePath == null || !resourcePath.contains("Patient")) {
			log.debug("Skipping transformation for non-Patient resource: " + resourcePath);
			return requestBody;
		}

		// Quick check if this is a Patient resource before parsing
		if (!requestBody.contains("\"resourceType\"") || !requestBody.contains("\"Patient\"")) {
			log.debug("Request body does not contain Patient resource, skipping transformation");
			return requestBody;
		}

		try {
			// Lazy initialization of FhirContext (thread-safe)
			if (fhirContext == null) {
				synchronized (this) {
					if (fhirContext == null) {
						fhirContext = FhirContext.forR4();
					}
				}
			}

			// Parse FHIR JSON
			IParser parser = fhirContext.newJsonParser();
			Patient patient = parser.parseResource(Patient.class, requestBody);

			// Extract UPI identifier value
			String upiValue = null;
			for (Identifier identifier : patient.getIdentifier()) {
				if ("UPI".equals(identifier.getSystem()) && identifier.hasValue()) {
					upiValue = identifier.getValue();
					log.debug("Found UPI identifier: " + upiValue);
					break;
				}
			}

			if (upiValue == null) {
				log.error("OpenHIM requires UPI identifier, but none found in Patient resource. " +
						 "Patient will likely be rejected by OpenHIM.");
				System.out.println(">>> OPENHIM PROXY: ERROR - No UPI identifier found in Patient resource!");
				// Return original body - let OpenHIM return the proper error message
				return requestBody;
			}

			// Set Patient.id to UPI value (OpenHIM requirement)
			patient.setId(upiValue);
			log.info("Transformed Patient.id to UPI value: " + upiValue + " for OpenHIM compatibility");
			System.out.println(">>> OPENHIM PROXY: Transformed Patient.id to UPI: " + upiValue);

			// Serialize back to JSON
			String transformedBody = parser.encodeResourceToString(patient);
			return transformedBody;

		} catch (Exception e) {
			log.warn("Failed to transform Patient resource for OpenHIM, forwarding as-is: " + e.getMessage());
			System.out.println(">>> OPENHIM PROXY: Transformation failed, forwarding original: " + e.getMessage());
			// Graceful fallback - forward original request
			return requestBody;
		}
	}

	/**
	 * Create Basic Authentication header
	 *
	 * Reuses pattern from FindUserFromNIDAByIdController
	 *
	 * @param username OpenHIM username
	 * @param password OpenHIM password
	 * @return Basic Auth header value (e.g., "Basic base64(username:password)")
	 */
	private String createBasicAuthHeader(String username, String password) {
		String plainCreds = username + ":" + password;
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);
		return "Basic " + base64Creds;
	}

	/**
	 * Create FHIR-compliant OperationOutcome for errors
	 *
	 * Used when proxy encounters configuration or network errors
	 * (not used for OpenHIM errors - those are forwarded as-is)
	 *
	 * @param status HTTP status code
	 * @param message Error message
	 * @return ResponseEntity with FHIR OperationOutcome
	 */
	private ResponseEntity<String> createErrorResponse(HttpStatus status, String message) {
		String severity = status.is5xxServerError() ? "error" : "warning";
		String operationOutcome = "{\n" +
			"  \"resourceType\": \"OperationOutcome\",\n" +
			"  \"issue\": [{\n" +
			"    \"severity\": \"" + severity + "\",\n" +
			"    \"code\": \"processing\",\n" +
			"    \"diagnostics\": \"" + escapeJson(message) + "\"\n" +
			"  }]\n" +
			"}";

		return ResponseEntity.status(status)
			.contentType(MediaType.APPLICATION_JSON)
			.body(operationOutcome);
	}

	/**
	 * Escape string for use in JSON value
	 *
	 * @param input String to escape
	 * @return JSON-safe string
	 */
	private String escapeJson(String input) {
		if (input == null) {
			return "";
		}
		return input.replace("\\", "\\\\")
				   .replace("\"", "\\\"")
				   .replace("\n", "\\n")
				   .replace("\r", "\\r")
				   .replace("\t", "\\t");
	}

	/**
	 * Decompress GZIP-encoded error response body
	 *
	 * RestTemplate automatically decompresses successful (2xx) responses,
	 * but error responses (4xx/5xx) thrown as exceptions are NOT decompressed.
	 * This method handles GZIP decompression for error response bodies.
	 *
	 * @param exception HTTP client or server error exception
	 * @return Decompressed response body as string
	 */
	private String getDecompressedErrorBody(HttpClientErrorException exception) {
		return decompressGzipIfNeeded(exception.getResponseBodyAsByteArray(),
									   exception.getResponseHeaders());
	}

	private String getDecompressedErrorBody(HttpServerErrorException exception) {
		return decompressGzipIfNeeded(exception.getResponseBodyAsByteArray(),
									   exception.getResponseHeaders());
	}

	/**
	 * Decompress byte array if Content-Encoding is gzip
	 *
	 * @param body Response body as byte array
	 * @param headers Response headers
	 * @return Decompressed string or original string if not GZIP
	 */
	private String decompressGzipIfNeeded(byte[] body, HttpHeaders headers) {
		if (body == null || body.length == 0) {
			return "";
		}

		// Check if response is GZIP-encoded
		String contentEncoding = headers.getFirst(HttpHeaders.CONTENT_ENCODING);
		if (contentEncoding != null && contentEncoding.toLowerCase().contains("gzip")) {
			try {
				ByteArrayInputStream bis = new ByteArrayInputStream(body);
				GZIPInputStream gis = new GZIPInputStream(bis);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();

				byte[] buffer = new byte[1024];
				int len;
				while ((len = gis.read(buffer)) > 0) {
					bos.write(buffer, 0, len);
				}

				gis.close();
				bos.close();

				return bos.toString("UTF-8");
			} catch (IOException e) {
				log.warn("Failed to decompress GZIP error response, returning raw bytes as string", e);
				return new String(body);
			}
		}

		// Not GZIP-encoded, return as-is
		return new String(body);
	}

	/**
	 * Apply identifier search fallback for OpenHIM compatibility (Auto-detection)
	 *
	 * AUTO-DETECTION STRATEGY:
	 * 1. Check if original response has zero results (total=0)
	 * 2. Check if query uses FHIR standard format (system|value)
	 * 3. If both true, retry with value-only format
	 * 4. Return whichever response succeeded
	 *
	 * This allows the proxy to work with:
	 * - FHIR-compliant servers (HAPI FHIR, etc.) → uses standard format, no retry
	 * - Non-compliant servers (OpenHIM/SanteMPI) → falls back automatically
	 * - Mixed environments → adapts per request
	 *
	 * Performance: Only retries on empty results, minimal overhead (~1ms check)
	 *
	 * @param originalResponse Response from first attempt (standard FHIR format)
	 * @param originalUri The URI that was called
	 * @param originalQueryString The query string used
	 * @param entity HTTP entity (headers, body)
	 * @param restTemplate RestTemplate for making retry request
	 * @param method HTTP method
	 * @return Best response (original if successful, fallback if original empty)
	 */
	private ResponseEntity<String> applyIdentifierSearchFallback(
			ResponseEntity<String> originalResponse,
			URI originalUri,
			String originalQueryString,
			HttpEntity<byte[]> entity,
			RestTemplate restTemplate,
			HttpMethod method) {

		try {
			// Check if original search returned zero results
			if (isEmptySearchResult(originalResponse.getBody())) {

				// Check if query has system|value format that can be transformed
				if (hasSystemValueFormat(originalQueryString)) {

					log.info("Standard identifier search returned 0 results, " +
							"attempting value-only fallback for OpenHIM compatibility");

					// Transform query to value-only format
					String transformedQuery = transformIdentifierSearchForOpenHIM(originalQueryString);

					// Build new URI with transformed query
					String baseUrl = originalUri.toString().split("\\?")[0];
					URI fallbackUri = UriComponentsBuilder.fromUriString(baseUrl)
						.query(transformedQuery)
						.build(true)
						.toUri();

					log.info("Retrying search with: " + fallbackUri);

					// Retry with transformed query
					ResponseEntity<String> fallbackResponse = restTemplate.exchange(
						fallbackUri, method, entity, String.class);

					// Remove Content-Encoding header from fallback response too
					HttpHeaders fallbackHeaders = new HttpHeaders();
					fallbackHeaders.putAll(fallbackResponse.getHeaders());
					fallbackHeaders.remove(HttpHeaders.CONTENT_ENCODING);

					// If fallback found results, use it
					if (!isEmptySearchResult(fallbackResponse.getBody())) {
						log.info("✅ Fallback search successful, found patient(s)");
						return ResponseEntity.status(fallbackResponse.getStatusCode())
							.headers(fallbackHeaders)
							.body(fallbackResponse.getBody());
					} else {
						log.debug("Fallback search also returned 0 results, patient does not exist");
					}
				}
			}
		} catch (Exception e) {
			log.warn("Fallback search failed, returning original response: " + e.getMessage());
		}

		// Return original response (either had results, or fallback failed/also empty)
		return originalResponse;
	}

	/**
	 * Check if FHIR search response has zero results
	 *
	 * @param responseBody FHIR Bundle JSON response
	 * @return true if total=0, false otherwise
	 */
	private boolean isEmptySearchResult(String responseBody) {
		if (responseBody == null || responseBody.isEmpty()) {
			return true;
		}
		// Simple JSON check - look for "total":0 or "total": 0
		return responseBody.contains("\"total\":0") || responseBody.contains("\"total\": 0");
	}

	/**
	 * Check if query string contains FHIR system|value format
	 *
	 * @param queryString URL query string
	 * @return true if contains pipe character (| or %7C)
	 */
	private boolean hasSystemValueFormat(String queryString) {
		return queryString != null && (queryString.contains("|") || queryString.contains("%7C"));
	}

	/**
	 * Transform FHIR identifier search to OpenHIM-compatible format
	 *
	 * WORKAROUND for OpenHIM/SanteMPI limitation:
	 * The FHIR R4 specification requires servers to support identifier searches
	 * in the format: identifier=system|value (where | is the pipe character).
	 * However, OpenHIM/SanteMPI only supports: identifier=value
	 *
	 * This method detects the FHIR standard format and extracts only the value part,
	 * making the search compatible with OpenHIM while keeping the Client Registry
	 * module standards-compliant.
	 *
	 * Examples:
	 * - Input:  identifier=http://clientregistry.org/openmrs|http://test.rwanda.emr/...
	 * - Output: identifier=http://test.rwanda.emr/...
	 *
	 * - Input:  identifier=http://clientregistry.org/openmrs%7Chttp://test.rwanda.emr/...
	 * - Output: identifier=http://test.rwanda.emr/...
	 *
	 * When OpenHIM implements proper FHIR R4 identifier search support,
	 * this transformation can be removed.
	 *
	 * @param queryString Original query string from FHIR client
	 * @return Transformed query string compatible with OpenHIM
	 */
	private String transformIdentifierSearchForOpenHIM(String queryString) {
		if (queryString == null || queryString.isEmpty()) {
			return queryString;
		}

		// Pattern matches: identifier=<system>|<value> or identifier=<system>%7C<value>
		// Where %7C is the URL-encoded pipe character
		// Captures everything before the pipe (system) and everything after (value)
		String pattern = "identifier=([^&|%]+)(%7C|\\|)([^&]+)";

		if (queryString.matches(".*" + pattern + ".*")) {
			// Replace with just the value part (capture group 3)
			String transformed = queryString.replaceAll(pattern, "identifier=$3");

			log.debug("Transformed identifier search: " + queryString + " → " + transformed);

			return transformed;
		}

		// No transformation needed - return original
		return queryString;
	}
}
