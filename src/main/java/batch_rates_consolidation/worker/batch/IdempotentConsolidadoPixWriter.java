package batch_rates_consolidation.worker.batch;

import batch_rates_consolidation.domain.ConsolidadoPix;
import batch_rates_consolidation.domain.ConsolidadoPixRepository;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class IdempotentConsolidadoPixWriter implements ItemWriter<ConsolidadoPix> {

	private final ConsolidadoPixRepository consolidadoPixRepository;

	public IdempotentConsolidadoPixWriter(ConsolidadoPixRepository consolidadoPixRepository) {
		this.consolidadoPixRepository = consolidadoPixRepository;
	}

	@Override
	public void write(Chunk<? extends ConsolidadoPix> chunk) {
		for (ConsolidadoPix consolidadoPix : chunk) {
			if (consolidadoPixRepository.findByEndToEndId(consolidadoPix.getEndToEndId()).isPresent()) {
				continue;
			}

			consolidadoPixRepository.save(consolidadoPix);
			consolidadoPix.getTransacaoPix().markConsolidated();
		}
	}
}
