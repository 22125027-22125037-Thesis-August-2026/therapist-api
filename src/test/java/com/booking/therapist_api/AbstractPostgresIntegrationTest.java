package com.booking.therapist_api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for {@code @SpringBootTest} integration tests.
 *
 * <p>These used to run on H2 with {@code MODE=PostgreSQL}. H2 cannot create the
 * {@code varchar[]} columns this schema relies on ({@code therapists.treated_challenges},
 * {@code patient_tags.tags}, {@code profiles_preferences.reasons}) — it rejects the generated
 * DDL with {@code Syntax error ... expected "(, ARRAY"}. Hibernate's {@code create-drop} logged
 * each failure as a WARN and carried on, so which <em>other</em> tables ended up missing depended
 * on schema-generation ordering that varied run to run. The symptom was a misleading
 * {@code Table "THERAPISTS" not found} on an INSERT unrelated to any array column.
 *
 * <p>Running the real database removes the entire failure mode, and running the real Flyway
 * migrations against it means these tests now exercise the schema the application actually
 * deploys — {@code ddl-auto: validate}, exactly as in production — rather than one Hibernate
 * regenerates from the entity mappings.
 *
 * <p><strong>Singleton container, started by hand.</strong> This deliberately does not use
 * {@code @Testcontainers}/{@code @Container}: that extension manages the lifecycle
 * <em>per test class</em>, so it stops the container when the first subclass finishes and every
 * later class fails with {@code Connection to localhost:<port> refused}. Starting it once in a
 * static initialiser and never stopping it gives all subclasses one shared instance for the whole
 * test JVM; Testcontainers' Ryuk sidecar removes it when the JVM exits.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:15-alpine");

    static {
        POSTGRES.start();
    }
}
