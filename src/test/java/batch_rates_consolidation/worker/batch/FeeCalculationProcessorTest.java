package batch_rates_consolidation.worker.batch;

import batch_rates_consolidation.domain.ConsolidadoPix;
import batch_rates_consolidation.domain.ConsolidadoPixRepository;
import batch_rates_consolidation.domain.TransacaoPix;
import batch_rates_consolidation.worker.service.FeeExemptionException;
import batch_rates_consolidation.worker.service.FeeExemptionService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeCalculationProcessorTest {

	@Mock
	private ConsolidadoPixRepository consolidadoPixRepository;

	@Mock
	private FeeExemptionService feeExemptionService;

	@Test
	void createsConsolidationWithDefaultFeeWhenTransactionIsNotExempt() {
		TransacaoPix transaction = transaction("E-success");
		when(consolidadoPixRepository.findByEndToEndId("E-success")).thenReturn(Optional.empty());
		when(feeExemptionService.isExempt(transaction)).thenReturn(false);

		ConsolidadoPix result = new FeeCalculationProcessor(
				consolidadoPixRepository,
				feeExemptionService,
				new BigDecimal("1.00"))
				.process(transaction);

		assertThat(result.getEndToEndId()).isEqualTo("E-success");
		assertThat(result.getTarifa()).isEqualByComparingTo("1.00");
	}

	@Test
	void propagatesExemptionFailureSoTheStepCanFail() {
		TransacaoPix transaction = transaction("E-failure");
		FeeExemptionException failure = new FeeExemptionException(
				"Exemption service unavailable",
				new IllegalStateException("timeout"));
		when(consolidadoPixRepository.findByEndToEndId("E-failure")).thenReturn(Optional.empty());
		when(feeExemptionService.isExempt(transaction)).thenThrow(failure);

		assertThatThrownBy(() -> new FeeCalculationProcessor(
				consolidadoPixRepository,
				feeExemptionService,
				new BigDecimal("1.00"))
				.process(transaction))
				.isSameAs(failure);
	}

	private TransacaoPix transaction(String endToEndId) {
		return new TransacaoPix(
				endToEndId,
				null,
				new BigDecimal("10.00"),
				OffsetDateTime.parse("2026-01-01T00:00:00Z"),
				null,
				JsonNodeFactory.instance.objectNode(),
				null,
				null);
	}
}
