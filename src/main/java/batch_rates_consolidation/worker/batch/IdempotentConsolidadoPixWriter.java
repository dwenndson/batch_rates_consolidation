package batch_rates_consolidation.worker.batch;

import batch_rates_consolidation.domain.ConsolidadoPix;
import batch_rates_consolidation.domain.ConsolidadoPixRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class IdempotentConsolidadoPixWriter implements ItemWriter<ConsolidadoPix> {

	private final ConsolidadoPixRepository consolidadoPixRepository;
	private final MeterRegistry meterRegistry;

	public IdempotentConsolidadoPixWriter(
			ConsolidadoPixRepository consolidadoPixRepository,
			MeterRegistry meterRegistry) {
		this.consolidadoPixRepository = consolidadoPixRepository;
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void write(Chunk<? extends ConsolidadoPix> chunk) {
		for (ConsolidadoPix consolidadoPix : chunk) {
			if (consolidadoPixRepository.findByEndToEndId(consolidadoPix.getEndToEndId()).isPresent()) {
				increment("duplicate");
				continue;
			}

			consolidadoPixRepository.save(consolidadoPix);
			consolidadoPix.getTransacaoPix().markConsolidated();
			increment("persisted");
		}
	}

	private void increment(String outcome) {
		Counter.builder("pix.consolidadas.total")
				.tag("profile", "worker")
				.tag("status", "completed")
				.tag("outcome", outcome)
				.register(meterRegistry)
				.increment();
	}
}
