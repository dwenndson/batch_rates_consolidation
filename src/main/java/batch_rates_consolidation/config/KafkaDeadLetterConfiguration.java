package batch_rates_consolidation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "batch.kafka.enabled", havingValue = "true")
public class KafkaDeadLetterConfiguration {

	@Bean
	CommonErrorHandler kafkaDeadLetterErrorHandler(
			KafkaTemplate<Object, Object> kafkaTemplate,
			@Value("${batch.kafka.dlt.backoff-interval:1000}") long backoffInterval,
			@Value("${batch.kafka.dlt.retry-limit:2}") long retryLimit) {
		return new DefaultErrorHandler(
				new DeadLetterPublishingRecoverer(kafkaTemplate),
				new FixedBackOff(backoffInterval, retryLimit));
	}
}
