package batch_rates_consolidation.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transacao_pix")
public class TransacaoPix {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "end_to_end_id", nullable = false, unique = true, length = 100)
	private String endToEndId;

	@Column(length = 100)
	private String txid;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal valor;

	@Column(nullable = false)
	private OffsetDateTime horario;

	@Column(length = 100)
	private String chave;

	@JdbcTypeCode(SqlTypes.JSON)
	private JsonNode pagador;

	@Column(name = "info_pagador")
	private String infoPagador;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private JsonNode devolucoes = JsonNodeFactory.instance.arrayNode();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TransacaoPixStatus status = TransacaoPixStatus.PENDING;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected TransacaoPix() {
	}

	public TransacaoPix(
			String endToEndId,
			String txid,
			BigDecimal valor,
			OffsetDateTime horario,
			String chave,
			JsonNode pagador,
			String infoPagador,
			JsonNode devolucoes) {
		this.endToEndId = endToEndId;
		this.txid = txid;
		this.valor = valor;
		this.horario = horario;
		this.chave = chave;
		this.pagador = pagador;
		this.infoPagador = infoPagador;
		this.devolucoes = devolucoes == null
				? JsonNodeFactory.instance.arrayNode()
				: devolucoes;
	}

	@PrePersist
	void initializeTimestamps() {
		OffsetDateTime now = OffsetDateTime.now();
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

	public BigDecimal getValor() {
		return valor;
	}

	public TransacaoPixStatus getStatus() {
		return status;
	}

	public void markProcessing() {
		status = TransacaoPixStatus.PROCESSING;
	}

	public void markConsolidated() {
		status = TransacaoPixStatus.CONSOLIDATED;
	}

	public void markRejected() {
		status = TransacaoPixStatus.REJECTED;
	}
}
