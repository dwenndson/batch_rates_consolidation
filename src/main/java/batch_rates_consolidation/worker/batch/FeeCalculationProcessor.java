package batch_rates_consolidation.worker.batch;

import batch_rates_consolidation.domain.ConsolidadoPix;
import batch_rates_consolidation.domain.ConsolidadoPixRepository;
import batch_rates_consolidation.domain.TransacaoPix;
import batch_rates_consolidation.domain.TransacaoPixStatus;
import batch_rates_consolidation.worker.service.FeeExemptionService;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.math.BigDecimal;

public class FeeCalculationProcessor implements ItemProcessor<TransacaoPix, ConsolidadoPix> {

	private final ConsolidadoPixRepository consolidadoPixRepository;
	private final FeeExemptionService feeExemptionService;
	private final BigDecimal defaultFee;

	public FeeCalculationProcessor(
			ConsolidadoPixRepository consolidadoPixRepository,
			FeeExemptionService feeExemptionService,
			BigDecimal defaultFee) {
		this.consolidadoPixRepository = consolidadoPixRepository;
		this.feeExemptionService = feeExemptionService;
		this.defaultFee = defaultFee;
	}

	@Override
	public ConsolidadoPix process(TransacaoPix transacaoPix) {
		if (transacaoPix.getStatus() != TransacaoPixStatus.PENDING) {
			return null;
		}

		if (consolidadoPixRepository.findByEndToEndId(transacaoPix.getEndToEndId()).isPresent()) {
			return null;
		}

		BigDecimal fee = feeExemptionService.isExempt(transacaoPix)
				? BigDecimal.ZERO
				: defaultFee;
		return new ConsolidadoPix(transacaoPix, fee);
	}
}
