package batch_rates_consolidation.manager.application;

import batch_rates_consolidation.manager.api.BatchExecutionRequest;
import batch_rates_consolidation.manager.api.BatchExecutionResponse;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SpringBatchExecutionService implements BatchExecutionService {

	private final JobLauncher jobLauncher;
	private final ObjectProvider<Job> jobProvider;

	public SpringBatchExecutionService(
			JobLauncher jobLauncher,
			@Qualifier("pixConsolidationJob") ObjectProvider<Job> jobProvider) {
		this.jobLauncher = jobLauncher;
		this.jobProvider = jobProvider;
	}

	@Override
	public BatchExecutionResponse start(BatchExecutionRequest request) {
		Job job = jobProvider.getIfAvailable();
		if (job == null) {
			throw new BatchJobNotConfiguredException();
		}

		JobParameters parameters = new JobParametersBuilder()
				.addLong("requestedAt", System.currentTimeMillis())
				.addLong("partitionSize", request.partitionSize().longValue())
				.toJobParameters();

		try {
			JobExecution execution = jobLauncher.run(job, parameters);
			return new BatchExecutionResponse(execution.getId(), execution.getStatus().name());
		} catch (Exception exception) {
			throw new BatchExecutionStartException(exception);
		}
	}
}
