package batch_rates_consolidation.manager.api;

public record BatchExecutionResponse(
		Long jobExecutionId,
		String status
) {
}
