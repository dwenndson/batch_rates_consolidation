package batch_rates_consolidation.manager.batch;

import batch_rates_consolidation.domain.TransacaoPix;
import batch_rates_consolidation.domain.TransacaoPixRepository;
import batch_rates_consolidation.domain.TransacaoPixStatus;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class PendingPixPartitioner implements Partitioner {

	private final TransacaoPixRepository transacaoPixRepository;
	private final long partitionSize;

	public PendingPixPartitioner(
			TransacaoPixRepository transacaoPixRepository,
			long partitionSize) {
		if (partitionSize < 1) {
			throw new IllegalArgumentException("partitionSize must be greater than zero");
		}
		this.transacaoPixRepository = transacaoPixRepository;
		this.partitionSize = partitionSize;
	}

	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		return transacaoPixRepository
				.findFirstByStatusOrderByIdAsc(TransacaoPixStatus.PENDING)
				.map(first -> createPartitions(first))
				.orElseGet(Map::of);
	}

	private Map<String, ExecutionContext> createPartitions(TransacaoPix first) {
		TransacaoPix last = transacaoPixRepository
				.findFirstByStatusOrderByIdDesc(TransacaoPixStatus.PENDING)
				.orElseThrow();

		Map<String, ExecutionContext> partitions = new LinkedHashMap<>();
		long partitionNumber = 0;
		for (long minId = first.getId(); minId <= last.getId(); minId += partitionSize) {
			long maxId = Math.min(minId + partitionSize - 1, last.getId());
			ExecutionContext context = new ExecutionContext();
			context.putLong("minId", minId);
			context.putLong("maxId", maxId);
			partitions.put("pix-" + partitionNumber++, context);
		}
		return partitions;
	}
}
