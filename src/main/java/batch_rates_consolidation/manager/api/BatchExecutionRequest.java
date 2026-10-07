package batch_rates_consolidation.manager.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BatchExecutionRequest(
		@NotNull
		@Min(1)
		@Max(1_000_000)
		Integer partitionSize
) {
}
