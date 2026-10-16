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
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "manager"})
class ManagerJobConfigurationTest {

	@Autowired
	@Qualifier("pixConsolidationJob")
	private Job pixConsolidationJob;

	@Autowired
	private JobLauncher jobLauncher;

	@Test
	void exposesPixConsolidationJob() {
		assertThat(pixConsolidationJob.getName()).isEqualTo("pixConsolidationJob");
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
