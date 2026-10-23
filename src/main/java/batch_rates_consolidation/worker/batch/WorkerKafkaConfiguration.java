package batch_rates_consolidation.worker.batch;

import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@Configuration
@Profile("worker")
@ConditionalOnProperty(name = "batch.kafka.enabled", havingValue = "true")
public class WorkerKafkaConfiguration {

	@Bean
	IntegrationFlow workerRequestsInboundFlow(
			@Qualifier("workerRequestsChannel") MessageChannel workerRequestsChannel,
			ConsumerFactory<String, Object> consumerFactory,
			CommonErrorHandler kafkaDeadLetterErrorHandler,
			@Value("${batch.kafka.requests-topic}") String requestsTopic,
			@Value("${spring.kafka.consumer.group-id}") String groupId) {
		return IntegrationFlow.from(
						Kafka.messageDrivenChannelAdapter(
								consumerFactory,
								containerProperties(requestsTopic, groupId))
								.configureListenerContainer(container ->
										container.errorHandler(kafkaDeadLetterErrorHandler)))
				.channel(workerRequestsChannel)
				.get();
	}

	@Bean
	IntegrationFlow workerRepliesOutboundFlow(
			@Qualifier("workerRepliesChannel") MessageChannel workerRepliesChannel,
			KafkaTemplate<String, Object> kafkaTemplate,
			@Value("${batch.kafka.replies-topic}") String repliesTopic) {
		return IntegrationFlow.from(workerRepliesChannel)
				.handle(Kafka.outboundChannelAdapter(kafkaTemplate)
						.topic(repliesTopic)
						.messageKey(this::replyKey))
				.get();
	}

	private ContainerProperties containerProperties(String topic, String groupId) {
		ContainerProperties properties = new ContainerProperties(topic);
		properties.setGroupId(groupId);
		properties.setAckMode(ContainerProperties.AckMode.RECORD);
		return properties;
	}

	private String replyKey(Message<StepExecution> message) {
		return String.valueOf(message.getPayload().getId());
	}
}
