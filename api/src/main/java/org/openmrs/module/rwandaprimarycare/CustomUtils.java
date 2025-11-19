package org.openmrs.module.rwandaprimarycare;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.util.UUID;

public class CustomUtils {

    protected static final Log log = LogFactory.getLog(CustomUtils.class);

    /**
     * Check if the system has internet connectivity
     *
     * @return true if online, false otherwise
     */
    public static boolean isOnline() {
        try {
            // Try to reach a reliable public DNS server
            InetAddress address = InetAddress.getByName("8.8.8.8");
            return address.isReachable(3000); // 3 second timeout
        } catch (Exception e) {
            log.warn("Internet connectivity check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generate a temporary patient identifier
     * Format: TEMP-{UUID}
     *
     * @return temporary identifier string
     */
    public static String generateTempId() {
        return "TEMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
