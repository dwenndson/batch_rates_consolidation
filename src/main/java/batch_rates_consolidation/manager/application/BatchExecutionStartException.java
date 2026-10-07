package batch_rates_consolidation.manager.application;

public class BatchExecutionStartException extends RuntimeException {

	public BatchExecutionStartException(Throwable cause) {
		super("Could not start the Pix consolidation Job", cause);
	}
}
