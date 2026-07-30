package com.booking.therapist_api;

import org.junit.jupiter.api.Test;

/**
 * Context-load smoke test. Inherits {@code @SpringBootTest}, the {@code test} profile and the
 * shared PostgreSQL container from {@link AbstractPostgresIntegrationTest}, so it boots against
 * the Flyway-migrated schema rather than a Hibernate-generated H2 one.
 */
class TherapistApiApplicationTests extends AbstractPostgresIntegrationTest {

	@Test
	void contextLoads() {
	}

}
