package batch_rates_consolidation.worker.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"worker", "test"})
@TestPropertySource(properties = "batch.kafka.enabled=false")
class WorkerJobConfigurationTest {

	@Autowired
	@Qualifier("workerProcessingStep")
	private Step workerProcessingStep;

	@Autowired
	@Qualifier("workerPartitionedStep")
	private Step workerPartitionedStep;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void exposesProcessingAndRemoteWorkerSteps() {
		assertThat(workerProcessingStep.getName()).isEqualTo("workerProcessingStep");
		assertThat(workerPartitionedStep.getName()).isEqualTo("workerPartitionedStep");
		assertThat(applicationContext.containsBean("workerRequestsInboundFlow")).isFalse();
		assertThat(applicationContext.containsBean("workerRepliesOutboundFlow")).isFalse();
	}
}
