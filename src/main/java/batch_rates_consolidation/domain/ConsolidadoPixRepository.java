package batch_rates_consolidation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsolidadoPixRepository extends JpaRepository<ConsolidadoPix, Long> {

	Optional<ConsolidadoPix> findByEndToEndId(String endToEndId);
}
