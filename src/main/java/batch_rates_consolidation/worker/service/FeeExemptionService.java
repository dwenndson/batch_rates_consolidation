package batch_rates_consolidation.worker.service;

import batch_rates_consolidation.domain.TransacaoPix;

public interface FeeExemptionService {

	boolean isExempt(TransacaoPix transacaoPix);
}
