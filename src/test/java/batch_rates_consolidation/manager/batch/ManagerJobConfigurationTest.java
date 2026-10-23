package batch_rates_consolidation.manager.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"manager", "test"})
@TestPropertySource(properties = "batch.kafka.enabled=false")
class ManagerJobConfigurationTest {

	@Autowired
	@Qualifier("pixConsolidationJob")
	private Job pixConsolidationJob;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void exposesPixConsolidationJob() {
		assertThat(pixConsolidationJob.getName()).isEqualTo("pixConsolidationJob");
		assertThat(applicationContext.containsBean("managerRequestsOutboundFlow")).isFalse();
		assertThat(applicationContext.containsBean("managerRepliesInboundFlow")).isFalse();
	}

	@Test
	void completesWhenThereAreNoPendingTransactions() throws Exception {
		JobExecution execution = jobLauncher.run(
				pixConsolidationJob,
				new JobParametersBuilder()
						.addLong("requestedAt", System.nanoTime())
						.addLong("partitionSize", 10L)
						.toJobParameters());

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
	}
}
