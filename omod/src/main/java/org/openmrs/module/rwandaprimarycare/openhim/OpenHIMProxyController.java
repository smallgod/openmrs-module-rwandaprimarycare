package org.openmrs.module.rwandaprimarycare.openhim;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenHIM Client Registry Proxy Controller
 *
 * Transparent HTTP proxy endpoint for forwarding FHIR Patient resources
 * from Client Registry module to OpenHIM.
 *
 * URL Pattern: /ws/rwandaprimarycare/openhim/**
 *
 * Supported FHIR Operations:
 * - GET  /Patient?family=Name  (search)
 * - GET  /Patient/{id}         (read)
 * - POST /Patient              (create)
 * - PUT  /Patient/{id}         (update)
 * - DELETE /Patient/{id}       (delete)
 *
 * Client Registry Configuration:
 *   clientregistry.clientRegistryServerUrl =
 *     "http://localhost:8080/openmrs/ws/rwandaprimarycare/openhim"
 *
 * Note: Uses /ws/ prefix (not /module/) to ensure Spring DispatcherServlet routing.
 * This follows the pattern used by openmrs-module-webservices.rest.
 *
 * @see /docs/OPENHIM_CLIENT_REGISTRY_PROXY.md for complete documentation
 */
@Controller
public class OpenHIMProxyController {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private OpenHIMClientRegistryProxy proxyService;

	/**
	 * Handle all HTTP methods and paths under /openhim/**
	 *
	 * Extracts request details and delegates to proxy service.
	 *
	 * Examples:
	 * - GET  /openhim/Patient?family=Man
	 * - GET  /openhim/Patient/123
	 * - POST /openhim/Patient
	 * - PUT  /openhim/Patient/123
	 *
	 * @param request HTTP servlet request
	 * @param body Request body (optional, used for POST/PUT)
	 * @return OpenHIM response (forwarded as-is)
	 */
	@RequestMapping(
		value = "/rwandaprimarycare/openhim/**",
		method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}
	)
	@ResponseBody
	public ResponseEntity<String> proxyRequest(
			HttpServletRequest request,
			@RequestBody(required = false) String body) {

		try {
			// 1. Extract HTTP method
			String httpMethod = request.getMethod();

			// 2. Extract resource path
			// Input:  /openmrs/ws/rwandaprimarycare/openhim/Patient/123
			// Output: /Patient/123
			String fullPath = request.getRequestURI();
			String resourcePath = extractResourcePath(fullPath);

			// 3. Extract query string
			// Input: family=Man&given=John
			String queryString = request.getQueryString();

			// 4. Extract headers
			Map<String, String> headers = extractHeaders(request);

			// 5. Log request
			log.info("Proxying " + httpMethod + " " + resourcePath +
					(queryString != null ? "?" + queryString : "") +
					(body != null ? " (with body)" : ""));

			// 6. Forward to OpenHIM via proxy service
			ResponseEntity<String> response = proxyService.forwardToOpenHIM(
				httpMethod, resourcePath, queryString, body, headers);

			// 7. Return response as-is
			return response;

		} catch (Exception e) {
			log.error("Error in proxy controller", e);
			return ResponseEntity.status(500)
				.body("{\"error\": \"Proxy error: " + escapeJson(e.getMessage()) + "\"}");
		}
	}

	/**
	 * Extract FHIR resource path from full URI
	 *
	 * Removes the proxy prefix to get the actual FHIR resource path.
	 *
	 * Input:  /openmrs/ws/rwandaprimarycare/openhim/Patient/123
	 * Output: /Patient/123
	 *
	 * @param fullPath Full request URI
	 * @return FHIR resource path
	 */
	private String extractResourcePath(String fullPath) {
		int openhimIndex = fullPath.indexOf("/openhim/");
		if (openhimIndex == -1) {
			// Fallback if pattern not found
			log.warn("Could not find /openhim/ in path: " + fullPath);
			return "/Patient"; // Default to /Patient
		}

		// Extract everything after "/openhim/"
		String resourcePath = fullPath.substring(openhimIndex + 8); // "/openhim/" is 8 chars

		// Ensure it starts with "/"
		if (!resourcePath.startsWith("/")) {
			resourcePath = "/" + resourcePath;
		}

		return resourcePath;
	}

	/**
	 * Extract request headers as Map
	 *
	 * Converts HttpServletRequest headers to a simple Map for proxy service.
	 *
	 * @param request HTTP servlet request
	 * @return Headers as Map<String, String>
	 */
	private Map<String, String> extractHeaders(HttpServletRequest request) {
		// Use TreeMap with CASE_INSENSITIVE_ORDER for RFC 7230 compliance
		// HTTP headers are case-insensitive, but HashMap is case-sensitive
		Map<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		Enumeration<String> headerNames = request.getHeaderNames();

		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			String headerValue = request.getHeader(headerName);
			headers.put(headerName, headerValue);
		}

		return headers;
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
