package batch_rates_consolidation.manager.application;

import batch_rates_consolidation.manager.api.BatchExecutionRequest;
import batch_rates_consolidation.manager.api.BatchExecutionResponse;

public interface BatchExecutionService {

	BatchExecutionResponse start(BatchExecutionRequest request);
}
