package batch_rates_consolidation.manager.batch;

import batch_rates_consolidation.domain.TransacaoPix;
import batch_rates_consolidation.domain.TransacaoPixRepository;
import batch_rates_consolidation.domain.TransacaoPixStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingPixPartitionerTest {

	@Mock
	private TransacaoPixRepository transacaoPixRepository;

	@Mock
	private TransacaoPix firstTransaction;

	@Mock
	private TransacaoPix lastTransaction;

	@Test
	void createsRangesFromPendingTransactionIds() {
		when(firstTransaction.getId()).thenReturn(10L);
		when(lastTransaction.getId()).thenReturn(25L);
		when(transacaoPixRepository.findFirstByStatusOrderByIdAsc(TransacaoPixStatus.PENDING))
				.thenReturn(Optional.of(firstTransaction));
		when(transacaoPixRepository.findFirstByStatusOrderByIdDesc(TransacaoPixStatus.PENDING))
				.thenReturn(Optional.of(lastTransaction));

		Map<String, ExecutionContext> partitions =
				new PendingPixPartitioner(transacaoPixRepository, 10).partition(1);

		assertThat(partitions).hasSize(2);
		assertThat(partitions.get("pix-0").getLong("minId")).isEqualTo(10L);
		assertThat(partitions.get("pix-0").getLong("maxId")).isEqualTo(19L);
		assertThat(partitions.get("pix-1").getLong("minId")).isEqualTo(20L);
		assertThat(partitions.get("pix-1").getLong("maxId")).isEqualTo(25L);
	}
}
