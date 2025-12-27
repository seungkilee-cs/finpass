package com.finpass.issuer.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.*;

/**
 * Simple database integration test
 * Tests that the application can connect to the configured database
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimpleDatabaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Should connect to database successfully")
    void shouldConnectToDatabaseSuccessfully() throws Exception {
        assertThat(dataSource).isNotNull();
        
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(5)).isTrue();
            
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(metaData).isNotNull();
            assertThat(metaData.getDatabaseProductName()).isNotBlank();
            assertThat(metaData.getDatabaseMajorVersion()).isPositive();
        }
    }

    @Test
    @DisplayName("Should validate database configuration")
    void shouldValidateDatabaseConfiguration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Verify we're using PostgreSQL (or the expected database)
            String productName = metaData.getDatabaseProductName();
            assertThat(productName).isIn("PostgreSQL", "H2"); // Allow for test databases
            
            // Verify connection URL is configured
            String url = metaData.getURL();
            assertThat(url).isNotBlank();
            assertThat(url).startsWith("jdbc:");
            
            // Verify transaction support
            assertThat(connection.getAutoCommit()).isFalse(); // @Transactional should handle this
        }
    }

    @Test
    @DisplayName("Should handle basic database operations")
    void shouldHandleBasicDatabaseOperations() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            // Test basic query execution
            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery("SELECT 1 as test_value")) {
                
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt("test_value")).isEqualTo(1);
            }
            
            // Test that we can create temporary tables (for test isolation)
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TEMPORARY TABLE test_table (id INTEGER, value VARCHAR(50))");
                statement.execute("INSERT INTO test_table (id, value) VALUES (1, 'test')");
                
                try (var resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM test_table")) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getInt("count")).isEqualTo(1);
                }
            }
        }
    }
}
