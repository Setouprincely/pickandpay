package com.doch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full context smoke test (Flyway migrations + Hibernate schema validation + bean wiring).
 * Needs a PostgreSQL database and the application environment variables, so it only runs
 * when DOCH_IT=true (see README "Running tests").
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DOCH_IT", matches = "true")
class DochApplicationTests {

	@Test
	void contextLoads() {
	}

}
