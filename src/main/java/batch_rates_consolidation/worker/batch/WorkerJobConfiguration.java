package batch_rates_consolidation.worker.batch;

import batch_rates_consolidation.domain.ConsolidadoPix;
import batch_rates_consolidation.domain.ConsolidadoPixRepository;
import batch_rates_consolidation.domain.TransacaoPix;
import batch_rates_consolidation.domain.TransacaoPixRepository;
import batch_rates_consolidation.worker.service.FeeExemptionService;
import batch_rates_consolidation.worker.service.FeeExemptionException;
import batch_rates_consolidation.worker.service.SimulatedFeeExemptionService;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.BeanFactoryStepLocator;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilder;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Map;

@Configuration
@Profile("worker")
@EnableBatchIntegration
public class WorkerJobConfiguration {

	@Bean
	MessageChannel workerRequestsChannel() {
		return new DirectChannel();
	}

	@Bean
	MessageChannel workerRepliesChannel() {
		return new DirectChannel();
	}

	@Bean
	BeanFactoryStepLocator workerStepLocator() {
		return new BeanFactoryStepLocator();
	}

	@Bean
	FeeExemptionService feeExemptionService() {
		return new SimulatedFeeExemptionService();
	}

	@Bean
	@org.springframework.batch.core.configuration.annotation.StepScope
	RepositoryItemReader<TransacaoPix> pixItemReader(
			TransacaoPixRepository transacaoPixRepository,
			@Value("#{stepExecutionContext['minId']}") Long minId,
			@Value("#{stepExecutionContext['maxId']}") Long maxId,
			@Value("${batch.chunk-size:100}") int chunkSize) {
		return new RepositoryItemReaderBuilder<TransacaoPix>()
				.name("pixItemReader")
				.repository(transacaoPixRepository)
				.methodName("findByIdBetweenOrderByIdAsc")
				.arguments(minId, maxId)
				.sorts(Map.of("id", Sort.Direction.ASC))
				.pageSize(chunkSize)
				.saveState(true)
				.build();
	}

	@Bean
	ItemProcessor<TransacaoPix, ConsolidadoPix> feeCalculationProcessor(
			ConsolidadoPixRepository consolidadoPixRepository,
			FeeExemptionService feeExemptionService,
			@Value("${batch.fee.default:1.00}") BigDecimal defaultFee) {
		return new FeeCalculationProcessor(
				consolidadoPixRepository,
				feeExemptionService,
				defaultFee);
	}

	@Bean
	ItemWriter<ConsolidadoPix> consolidadoPixWriter(
			ConsolidadoPixRepository consolidadoPixRepository) {
		return new IdempotentConsolidadoPixWriter(consolidadoPixRepository);
	}

	@Bean
	Step workerProcessingStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			@Qualifier("pixItemReader") ItemReader<TransacaoPix> reader,
			@Qualifier("feeCalculationProcessor")
			ItemProcessor<TransacaoPix, ConsolidadoPix> processor,
			@Qualifier("consolidadoPixWriter") ItemWriter<ConsolidadoPix> writer,
			@Value("${batch.chunk-size:100}") int chunkSize) {
		return new org.springframework.batch.core.step.builder.StepBuilder(
				"workerProcessingStep",
				jobRepository)
				.<TransacaoPix, ConsolidadoPix>chunk(chunkSize, transactionManager)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.faultTolerant()
				.retryLimit(3)
				.retry(FeeExemptionException.class)
				.build();
	}

	@Bean
	Step workerPartitionedStep(
			JobRepository jobRepository,
			@Qualifier("workerRequestsChannel") MessageChannel requestsChannel,
			@Qualifier("workerRepliesChannel") MessageChannel repliesChannel,
			BeanFactoryStepLocator workerStepLocator,
			BeanFactory beanFactory,
			PlatformTransactionManager transactionManager) {
		return new RemotePartitioningWorkerStepBuilder(
				"workerPartitionedStep",
				jobRepository)
				.inputChannel(requestsChannel)
				.outputChannel(repliesChannel)
				.stepLocator(workerStepLocator)
				.beanFactory(beanFactory)
				.tasklet(
						(contribution, chunkContext) ->
								org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED,
						transactionManager)
				.build();
	}
}
