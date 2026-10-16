package batch_rates_consolidation.manager.batch;

import batch_rates_consolidation.domain.TransacaoPixRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

@Configuration
@Profile("manager")
@EnableBatchIntegration
public class ManagerJobConfiguration {

	@Bean
	@StepScope
	PendingPixPartitioner pendingPixPartitioner(
			TransacaoPixRepository transacaoPixRepository,
			@Value("#{jobParameters['partitionSize']}") Long partitionSize) {
		return new PendingPixPartitioner(transacaoPixRepository, partitionSize);
	}

	@Bean
	MessageChannel batchRequestsChannel() {
		return new DirectChannel();
	}

	@Bean
	PollableChannel batchRepliesChannel() {
		return new QueueChannel();
	}

	@Bean
	Step managerPartitionedStep(
			JobRepository jobRepository,
			PendingPixPartitioner pendingPixPartitioner,
			MessageChannel batchRequestsChannel,
			PollableChannel batchRepliesChannel,
			@Value("${batch.partition.grid-size:1}") int gridSize) {
		return new RemotePartitioningManagerStepBuilder(
				"managerPartitionedStep",
				jobRepository)
				.partitioner("workerProcessingStep", pendingPixPartitioner)
				.gridSize(gridSize)
				.outputChannel(batchRequestsChannel)
				.inputChannel(batchRepliesChannel)
				.build();
	}

	@Bean(name = "pixConsolidationJob")
	Job pixConsolidationJob(JobRepository jobRepository, Step managerPartitionedStep) {
		return new JobBuilder("pixConsolidationJob", jobRepository)
				.start(managerPartitionedStep)
				.build();
	}
}
