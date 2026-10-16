package batch_rates_consolidation.worker.service;

import batch_rates_consolidation.domain.TransacaoPix;

public class SimulatedFeeExemptionService implements FeeExemptionService {

	@Override
	public boolean isExempt(TransacaoPix transacaoPix) {
		return false;
	}
}
