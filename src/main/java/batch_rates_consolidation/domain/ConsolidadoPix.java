package batch_rates_consolidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "consolidado_pix")
public class ConsolidadoPix {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transacao_pix_id", nullable = false, unique = true)
	private TransacaoPix transacaoPix;

	@Column(name = "end_to_end_id", nullable = false, unique = true, length = 100)
	private String endToEndId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal tarifa;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ConsolidadoPixStatus status;

	@Column(name = "consolidated_at", nullable = false)
	private OffsetDateTime consolidatedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected ConsolidadoPix() {
	}

	public ConsolidadoPix(TransacaoPix transacaoPix, BigDecimal tarifa) {
		this.transacaoPix = transacaoPix;
		this.endToEndId = transacaoPix.getEndToEndId();
		this.tarifa = tarifa;
		this.status = ConsolidadoPixStatus.COMPLETED;
	}

	@PrePersist
	void initializeTimestamps() {
		OffsetDateTime now = OffsetDateTime.now();
		consolidatedAt = now;
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void updateTimestamp() {
		updatedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getEndToEndId() {
		return endToEndId;
	}

	public TransacaoPix getTransacaoPix() {
		return transacaoPix;
	}

	public BigDecimal getTarifa() {
		return tarifa;
	}

	public ConsolidadoPixStatus getStatus() {
		return status;
	}
}
