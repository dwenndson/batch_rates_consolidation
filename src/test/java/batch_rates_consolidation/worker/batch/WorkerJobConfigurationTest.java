package batch_rates_consolidation.worker.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "worker"})
class WorkerJobConfigurationTest {

	@Autowired
	@Qualifier("workerProcessingStep")
	private Step workerProcessingStep;

	@Autowired
	@Qualifier("workerPartitionedStep")
	private Step workerPartitionedStep;

	@Test
	void exposesProcessingAndRemoteWorkerSteps() {
		assertThat(workerProcessingStep.getName()).isEqualTo("workerProcessingStep");
		assertThat(workerPartitionedStep.getName()).isEqualTo("workerPartitionedStep");
	}
}
