package com.awn.bracket.shared.utils;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

@Unremovable
@ApplicationScoped
public class DataCleaning {

    private static final Logger LOG = Logger.getLogger(DataCleaning.class);

    private static final List<String> APP_SCHEMAS = List.of("bucket", "bracket");

    private static final List<String> PROTECTED_TABLES = List.of("flyway_schema_history", "schema_bootstrap");

    private static final String TEST_DB_MARKER = "bracket_test";

    @ConfigProperty(name = "quarkus.datasource.jdbc.url", defaultValue = "")
    String jdbcUrl;

    @PersistenceContext
    EntityManager em;

    @Transactional
    public int cleanTestDatabase() {
        assertTestDatabase();
        int total = 0;
        for (String schema : APP_SCHEMAS) {
            total += truncateSchema(schema);
        }
        LOG.infof("test data cleaned: %d table(s) truncated across schemas %s", total, APP_SCHEMAS);
        return total;
    }

    private int truncateSchema(String schema) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<String> tables = (List<String>) em.createNativeQuery(
                        "SELECT table_name FROM information_schema.tables "
                      + "WHERE table_schema = ?1 AND table_type = 'BASE TABLE'")
                .setParameter(1, schema)
                .getResultList()
                .stream()
                .map(Object::toString)
                .filter(t -> !PROTECTED_TABLES.contains(t))
                .toList();

        if (tables.isEmpty()) {
            return 0;
        }

        String csv = tables.stream()
                .map(t -> "\"" + schema + "\".\"" + t + "\"")
                .collect(Collectors.joining(", "));

        em.createNativeQuery("TRUNCATE TABLE " + csv + " RESTART IDENTITY CASCADE").executeUpdate();
        LOG.debugf("truncated %d table(s) in schema '%s': %s", tables.size(), schema, tables);
        return tables.size();
    }

    private void assertTestDatabase() {
        if (jdbcUrl == null || !jdbcUrl.contains(TEST_DB_MARKER)) {
            throw new IllegalStateException(
                    "DataCleaning is only allowed against the test database ('" + TEST_DB_MARKER
                  + "'), but the datasource URL is: " + jdbcUrl
                  + " — refusing to truncate data.");
        }
    }
}
