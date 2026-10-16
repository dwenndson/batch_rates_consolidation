package batch_rates_consolidation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransacaoPixRepository extends JpaRepository<TransacaoPix, Long> {

	Optional<TransacaoPix> findFirstByStatusOrderByIdAsc(TransacaoPixStatus status);

	Optional<TransacaoPix> findFirstByStatusOrderByIdDesc(TransacaoPixStatus status);

	Page<TransacaoPix> findByIdBetweenOrderByIdAsc(
			Long minId,
			Long maxId,
			Pageable pageable);

	Page<TransacaoPix> findByStatusAndIdBetweenOrderByIdAsc(
			TransacaoPixStatus status,
			Long minId,
			Long maxId,
			Pageable pageable);
}
