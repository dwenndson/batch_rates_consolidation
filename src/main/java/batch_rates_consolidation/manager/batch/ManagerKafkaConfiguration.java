package batch_rates_consolidation.manager.batch;

import org.springframework.batch.integration.partition.StepExecutionRequest;
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
import org.springframework.messaging.PollableChannel;

@Configuration
@Profile("manager")
@ConditionalOnProperty(name = "batch.kafka.enabled", havingValue = "true")
public class ManagerKafkaConfiguration {

	@Bean
	IntegrationFlow managerRequestsOutboundFlow(
			@Qualifier("batchRequestsChannel") MessageChannel batchRequestsChannel,
			KafkaTemplate<String, Object> kafkaTemplate,
			@Value("${batch.kafka.requests-topic}") String requestsTopic) {
		return IntegrationFlow.from(batchRequestsChannel)
				.handle(Kafka.outboundChannelAdapter(kafkaTemplate)
						.topic(requestsTopic)
						.messageKey(this::requestKey))
				.get();
	}

	@Bean
	IntegrationFlow managerRepliesInboundFlow(
			@Qualifier("batchRepliesChannel") PollableChannel batchRepliesChannel,
			ConsumerFactory<String, Object> consumerFactory,
			CommonErrorHandler kafkaDeadLetterErrorHandler,
			@Value("${batch.kafka.replies-topic}") String repliesTopic,
			@Value("${spring.application.name}") String applicationName) {
		return IntegrationFlow.from(
						Kafka.messageDrivenChannelAdapter(
								consumerFactory,
								containerProperties(repliesTopic, applicationName + "-replies"))
								.configureListenerContainer(container ->
										container.errorHandler(kafkaDeadLetterErrorHandler)))
				.channel(batchRepliesChannel)
				.get();
	}

	private ContainerProperties containerProperties(String topic, String groupId) {
		ContainerProperties properties = new ContainerProperties(topic);
		properties.setGroupId(groupId);
		properties.setAckMode(ContainerProperties.AckMode.RECORD);
		return properties;
	}

	private String requestKey(Message<StepExecutionRequest> message) {
		return String.valueOf(message.getPayload().getStepExecutionId());
	}
}
