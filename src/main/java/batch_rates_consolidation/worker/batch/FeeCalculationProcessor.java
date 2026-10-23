package batch_rates_consolidation.worker.batch;

import batch_rates_consolidation.domain.ConsolidadoPix;
import batch_rates_consolidation.domain.ConsolidadoPixRepository;
import batch_rates_consolidation.domain.TransacaoPix;
import batch_rates_consolidation.domain.TransacaoPixStatus;
import batch_rates_consolidation.worker.service.FeeExemptionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.math.BigDecimal;

public class FeeCalculationProcessor implements ItemProcessor<TransacaoPix, ConsolidadoPix> {

	private final ConsolidadoPixRepository consolidadoPixRepository;
	private final FeeExemptionService feeExemptionService;
	private final BigDecimal defaultFee;
	private final MeterRegistry meterRegistry;

	public FeeCalculationProcessor(
			ConsolidadoPixRepository consolidadoPixRepository,
			FeeExemptionService feeExemptionService,
			BigDecimal defaultFee,
			MeterRegistry meterRegistry) {
		this.consolidadoPixRepository = consolidadoPixRepository;
		this.feeExemptionService = feeExemptionService;
		this.defaultFee = defaultFee;
		this.meterRegistry = meterRegistry;
	}

	@Override
	public ConsolidadoPix process(TransacaoPix transacaoPix) {
		if (transacaoPix.getStatus() != TransacaoPixStatus.PENDING) {
			return null;
		}

		if (consolidadoPixRepository.findByEndToEndId(transacaoPix.getEndToEndId()).isPresent()) {
			return null;
		}

		Timer.Sample sample = Timer.start(meterRegistry);
		String status = "success";
		String outcome = "charged";
		try {
			boolean exempt = feeExemptionService.isExempt(transacaoPix);
			outcome = exempt ? "exempt" : "charged";
			BigDecimal fee = exempt ? BigDecimal.ZERO : defaultFee;
			return new ConsolidadoPix(transacaoPix, fee);
		}
		catch (RuntimeException exception) {
			status = "failed";
			outcome = "error";
			throw exception;
		}
		finally {
			sample.stop(Timer.builder("pix.processamento.tempo")
					.tag("profile", "worker")
					.tag("status", status)
					.tag("outcome", outcome)
					.register(meterRegistry));
		}
	}
}
