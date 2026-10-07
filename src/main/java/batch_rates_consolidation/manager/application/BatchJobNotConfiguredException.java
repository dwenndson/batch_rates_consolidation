package batch_rates_consolidation.manager.application;

public class BatchJobNotConfiguredException extends RuntimeException {

	public BatchJobNotConfiguredException() {
		super("The Pix consolidation Job is not configured yet");
	}
}
