package batch_rates_consolidation.domain;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransacaoPixRepositoryTest {

	@Autowired
	private TransacaoPixRepository transacaoPixRepository;

	@Autowired
	private ConsolidadoPixRepository consolidadoPixRepository;

	@Test
	void findsPendingTransactionsWithinPartition() {
		TransacaoPix first = transacaoPixRepository.save(transaction("E-FIRST"));
		TransacaoPix second = transacaoPixRepository.save(transaction("E-SECOND"));
		TransacaoPix outsidePartition = transacaoPixRepository.save(transaction("E-OUTSIDE"));

		var result = transacaoPixRepository.findByStatusAndIdBetweenOrderByIdAsc(
				TransacaoPixStatus.PENDING,
				first.getId(),
				second.getId(),
				PageRequest.of(0, 10));

		assertThat(result.getContent())
				.extracting(TransacaoPix::getEndToEndId)
				.containsExactly("E-FIRST", "E-SECOND")
				.doesNotContain(outsidePartition.getEndToEndId());
	}

	@Test
	void findsConsolidatedTransactionByNaturalKey() {
		TransacaoPix transaction = transacaoPixRepository.save(transaction("E-CONSOLIDATED"));
		ConsolidadoPix consolidated = consolidadoPixRepository.save(
				new ConsolidadoPix(transaction, new BigDecimal("1.25")));

		assertThat(consolidadoPixRepository.findByEndToEndId("E-CONSOLIDATED"))
				.contains(consolidated);
	}

	private TransacaoPix transaction(String endToEndId) {
		return new TransacaoPix(
				endToEndId,
				null,
				new BigDecimal("100.00"),
				OffsetDateTime.parse("2026-01-01T12:00:00Z"),
				null,
				JsonNodeFactory.instance.objectNode(),
				null,
				JsonNodeFactory.instance.arrayNode());
	}
}
