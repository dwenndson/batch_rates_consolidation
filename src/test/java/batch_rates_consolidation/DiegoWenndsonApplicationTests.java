package batch_rates_consolidation;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class DiegoWenndsonApplicationTests {

	@Autowired
	private JobRepository jobRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void configuresJobRepository() {
		assertNotNull(jobRepository);
	}

	@Test
	void autoConfiguresFlywayAndDiscoversMigrations() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						DataSourceAutoConfiguration.class,
						FlywayAutoConfiguration.class))
				.withPropertyValues(
						"spring.datasource.url=jdbc:h2:mem:flyway_check;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
						"spring.datasource.username=sa",
						"spring.flyway.enabled=true",
						"spring.flyway.locations=classpath:db/migration")
				.withBean(FlywayMigrationStrategy.class, () -> flyway -> {})
				.run(context -> {
					assertThat(context).hasSingleBean(Flyway.class);
					Flyway flyway = context.getBean(Flyway.class);
					assertThat(flyway.info().all())
							.extracting(MigrationInfo::getScript)
							.containsExactly(
									"V1__create_spring_batch_metadata.sql",
									"V2__create_pix_tables.sql",
									"V3__seed_initial_pix_transactions.sql");
				});
	}
}
