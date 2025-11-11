package org.openmrs.module.rwandaprimarycare.openhim;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * OpenHIM Client Registry Proxy Service
 *
 * Transparent HTTP proxy that forwards FHIR Patient resources from Client Registry module
 * to OpenHIM for enterprise health information exchange.
 *
 * This is a pure pass-through implementation - no request/response transformation.
 *
 * @see /docs/OPENHIM_CLIENT_REGISTRY_PROXY.md for complete documentation
 */
@Service
public class OpenHIMClientRegistryProxy {

	protected final Log log = LogFactory.getLog(getClass());

	/**
	 * HTTP headers to copy from original request to OpenHIM request
	 */
	private static final List<String> HEADERS_TO_COPY = Arrays.asList(
		"Content-Type",
		"Accept",
		"Accept-Encoding",
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

			// 2. Build target URL (preserve path and query parameters)
			String targetUrl = openhimBaseUrl + resourcePath;
			if (queryString != null && !queryString.isEmpty()) {
				targetUrl += "?" + queryString;
			}

			log.info("Forwarding " + httpMethod + " request to OpenHIM: " + targetUrl);

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

			// 4. Create request entity
			// Use byte[] instead of String to prevent RestTemplate's StringHttpMessageConverter
			// from overriding Content-Type header to text/plain
			HttpEntity<byte[]> entity;
			if (requestBody != null && !requestBody.isEmpty()) {
				try {
					byte[] bodyBytes = requestBody.getBytes("UTF-8");
					entity = new HttpEntity<byte[]>(bodyBytes, headers);
				} catch (UnsupportedEncodingException e) {
					// UTF-8 is always supported per Java spec, this should never happen
					log.error("Failed to convert request body to UTF-8 bytes (should never occur)", e);
					// Fallback with explicit UTF-8 to prevent platform-dependent encoding
					try {
						entity = new HttpEntity<byte[]>(requestBody.getBytes("UTF-8"), headers);
					} catch (UnsupportedEncodingException uee) {
						// Last resort - use platform default (should never reach here)
						entity = new HttpEntity<byte[]>(requestBody.getBytes(), headers);
					}
				}
			} else {
				entity = new HttpEntity<byte[]>(headers);
			}

			// 5. Forward to OpenHIM
			RestTemplate restTemplate = new RestTemplate();
			HttpMethod method = HttpMethod.valueOf(httpMethod.toUpperCase());

			ResponseEntity<String> response = restTemplate.exchange(
				targetUrl, method, entity, String.class);

			log.info("OpenHIM response: " + response.getStatusCode());

			// 6. Return response as-is
			return response;

		} catch (HttpClientErrorException e) {
			// 4xx errors from OpenHIM (bad request, not found, etc.)
			// Forward OpenHIM's error response to client
			log.error("OpenHIM client error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
			return ResponseEntity.status(e.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(e.getResponseBodyAsString());

		} catch (HttpServerErrorException e) {
			// 5xx errors from OpenHIM (server error)
			// Forward OpenHIM's error response to client
			log.error("OpenHIM server error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
			return ResponseEntity.status(e.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(e.getResponseBodyAsString());

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
}
