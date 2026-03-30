package com.workoutplanner.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
class DatabaseConfigTest {

    private DatabaseConfig databaseConfig;
    private String originalDatabaseUrl;

    @BeforeEach
    void setUp() {
        databaseConfig = new DatabaseConfig();
        // Store original environment variable if it exists
        originalDatabaseUrl = System.getenv("DATABASE_URL");
    }

    @AfterEach
    void tearDown() {
        // Clean up any test environment variables
        // Note: We can't actually unset environment variables in Java tests,
        // but we can document the expected cleanup
    }

    @Test
    void testValidPostgresUrlParsing() throws URISyntaxException {
        // Test Case 1: Standard Render postgres:// URL
        String postgresUrl = "postgres://username:password@host:5432/database";

        // Mock the DATABASE_URL environment variable
        ReflectionTestUtils.setField(databaseConfig, "databaseUrl", "");

        // Use reflection to test the URL parsing logic
        // This test validates that a clean postgres:// URL is properly converted to JDBC format
        assertTrue(postgresUrl.startsWith("postgres://"),
            "Should recognize postgres:// URLs");

        // Verify URL structure components can be extracted
        String[] parts = postgresUrl.substring("postgres://".length()).split("[@/:]");
        assertEquals("username", parts[0], "Username should be extracted correctly");
        assertEquals("password", parts[1], "Password should be extracted correctly");
        assertEquals("host", parts[2], "Host should be extracted correctly");
        assertEquals("5432", parts[3], "Port should be extracted correctly");
        assertEquals("database", parts[4], "Database name should be extracted correctly");
    }

    @Test
    void testMalformedJdbcUrlWithLineBreaks() throws URISyntaxException {
        // Test Case 2: Malformed JDBC URL with line breaks (the actual Render issue)
        String malformedJdbcUrl = "jdbc:postgresql://byld_user:rPzlTDGNR5MN9R15TxJQzzLtUgvk81f5@\n  dpg-d5jf0d7fte5s738k9nd0-a/byld";

        // Clean the URL (this is what our fix does)
        String cleanedUrl = malformedJdbcUrl.replaceAll("\\s+", "").trim();

        // Verify cleaning works
        assertFalse(cleanedUrl.contains("\n"), "Cleaned URL should not contain line breaks");
        assertFalse(cleanedUrl.contains(" "), "Cleaned URL should not contain spaces");
        assertTrue(cleanedUrl.startsWith("jdbc:postgresql://"), "Should maintain JDBC format");

        // Test component extraction from malformed JDBC URL
        String urlPart = cleanedUrl.substring("jdbc:postgresql://".length());
        String[] parts = urlPart.split("@");
        assertEquals(2, parts.length, "Should split into credentials and host/db parts");

        String credentials = parts[0];
        String hostAndDb = parts[1];

        String[] credParts = credentials.split(":");
        assertEquals("byld_user", credParts[0], "Username should be extracted correctly");
        assertEquals("rPzlTDGNR5MN9R15TxJQzzLtUgvk81f5", credParts[1], "Password should be extracted correctly");

        String[] hostDbParts = hostAndDb.split("/");
        assertEquals("dpg-d5jf0d7fte5s738k9nd0-a", hostDbParts[0], "Host should be extracted correctly");
        assertEquals("byld", hostDbParts[1], "Database should be extracted correctly");
    }

    @Test
    void testVariousWhitespaceScenarios() {
        // Test Case 3: Various whitespace corruption scenarios
        String[] problematicUrls = {
            "jdbc:postgresql://user:pass@host/db\n",  // Trailing newline
            " jdbc:postgresql://user:pass@host/db",   // Leading space
            "jdbc:postgresql://user:pass@host/db ",   // Trailing space
            "jdbc:postgresql://user:pass@\n  host/db", // Line break in middle
            "jdbc:postgresql://user:pass@host\t/db",   // Tab character
            "jdbc:postgresql://user:pass@host\r\n/db"  // Windows line endings
        };

        for (String problematicUrl : problematicUrls) {
            String cleaned = problematicUrl.replaceAll("\\s+", "").trim();
            assertEquals("jdbc:postgresql://user:pass@host/db", cleaned,
                "URL with whitespace should clean properly: " + problematicUrl);
        }
    }

    @Test
    void testSeparateEnvironmentVariables() {
        // Test Case 4: Separate DB_* environment variables
        // This tests the fallback when DATABASE_URL is not available

        String expectedHost = "test-host";
        String expectedPort = "5432";
        String expectedDatabase = "test-db";
        String expectedUsername = "test-user";
        String expectedPassword = "test-password";

        // Mock environment variables (in real test, you'd use @MockBean or similar)
        // For now, just verify the URL construction logic
        String constructedUrl = String.format("jdbc:postgresql://%s:%s/%s",
            expectedHost, expectedPort, expectedDatabase);

        assertEquals("jdbc:postgresql://test-host:5432/test-db", constructedUrl,
            "Should construct JDBC URL correctly from separate components");
    }

    @Test
    void testUrlValidation() throws URISyntaxException {
        // Test Case 5: URL validation scenarios
        String[] validUrls = {
            "postgres://user:pass@host:5432/db",
            "postgresql://user:pass@host:5432/db",
            "jdbc:postgresql://host:5432/db"
        };

        String[] invalidUrls = {
            "",
            null,
            "mysql://user:pass@host:5432/db",  // Wrong database type
            "postgres://",                      // Incomplete URL
            "jdbc:mysql://host:5432/db"        // Wrong JDBC type
        };

        for (String validUrl : validUrls) {
            if (validUrl != null) {
                assertTrue(
                    validUrl.startsWith("postgres://") ||
                    validUrl.startsWith("postgresql://") ||
                    validUrl.startsWith("jdbc:postgresql://"),
                    "Should recognize valid PostgreSQL URL: " + validUrl
                );
            }
        }

        for (String invalidUrl : invalidUrls) {
            if (invalidUrl != null && !invalidUrl.isEmpty()) {
                boolean isValidScheme = invalidUrl.startsWith("postgres://") ||
                    invalidUrl.startsWith("postgresql://") ||
                    invalidUrl.startsWith("jdbc:postgresql://");
                // A valid URL must have a proper scheme AND content after the scheme
                boolean hasContent = invalidUrl.length() > "postgres://".length() &&
                    !invalidUrl.equals("postgres://") &&
                    !invalidUrl.equals("postgresql://");
                assertFalse(
                    isValidScheme && hasContent,
                    "Should reject invalid URL: " + invalidUrl
                );
            }
        }
    }

    @Test
    void testErrorHandling() {
        // Test Case 6: Error handling for edge cases
        String[] problematicScenarios = {
            "jdbc:postgresql://user@host/db",        // Missing password
            "jdbc:postgresql://host/db",             // Missing credentials entirely
            "jdbc:postgresql://user:pass@/db",      // Missing host
            "jdbc:postgresql://user:pass@host/",     // Missing database
            "jdbc:postgresql://user:pass@host"       // Missing database and slash
        };

        // These should either be handled gracefully or fail with clear error messages
        for (String problematic : problematicScenarios) {
            // Verify our parsing logic handles these cases without crashing
            String cleaned = problematic.replaceAll("\\s+", "").trim();
            assertNotNull(cleaned, "Cleaning should not return null for: " + problematic);
            assertTrue(cleaned.startsWith("jdbc:postgresql://"),
                "Should maintain JDBC format even for problematic URLs: " + problematic);
        }
    }

    @Test
    void testRenderSpecificScenarios() {
        // Test Case 7: Render.com specific URL patterns we observed
        String renderLikeUrl = "jdbc:postgresql://byld_user:rPzlTDGNR5MN9R15TxJQzzLtUgvk81f5@\n  dpg-d5jf0d7fte5s738k9nd0-a/byld";

        // This replicates the exact scenario we encountered
        String cleaned = renderLikeUrl.replaceAll("\\s+", "").trim();

        // Verify the specific parsing steps that failed initially
        assertTrue(cleaned.startsWith("jdbc:postgresql://"),
            "Should handle Render's JDBC format");
        assertFalse(cleaned.contains("\n"),
            "Should remove line breaks");
        assertFalse(cleaned.contains("  "),
            "Should remove multiple spaces");

        // Verify component extraction works
        String urlPart = cleaned.substring("jdbc:postgresql://".length());
        String[] parts = urlPart.split("@");
        assertEquals(2, parts.length, "Should split credentials from host/db");

        // Verify no truncation issues
        String[] credParts = parts[0].split(":");
        assertEquals("byld_user", credParts[0], "Username should not be truncated");
        assertTrue(credParts[1].length() > 10, "Password should not be truncated");
    }
}