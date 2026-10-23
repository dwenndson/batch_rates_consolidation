package batch_rates_consolidation.worker.service;

import batch_rates_consolidation.domain.TransacaoPix;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpFeeExemptionServiceTest {

	private MockRestServiceServer server;
	private HttpFeeExemptionService service;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://exemption");
		server = MockRestServiceServer.bindTo(builder).build();
		service = new HttpFeeExemptionService(builder.build(), "/exemptions/{endToEndId}");
	}

	@Test
	void returnsTrueWhenTheServiceMarksTheTransactionExempt() {
		server.expect(requestTo("http://exemption/exemptions/E-exempt"))
				.andRespond(withSuccess("{\"exempt\":true}", MediaType.APPLICATION_JSON));

		assertThat(service.isExempt(transaction("E-exempt"))).isTrue();
		server.verify();
	}

	@Test
	void treatsMissingExemptionAsNotExempt() {
		server.expect(requestTo("http://exemption/exemptions/E-missing"))
				.andRespond(withResourceNotFound());

		assertThat(service.isExempt(transaction("E-missing"))).isFalse();
		server.verify();
	}

	@Test
	void wrapsServerFailuresAsRetryableExemptionErrors() {
		server.expect(requestTo("http://exemption/exemptions/E-timeout"))
				.andRespond(withServerError());

		assertThatThrownBy(() -> service.isExempt(transaction("E-timeout")))
				.isInstanceOf(FeeExemptionException.class)
				.hasMessage("Exemption service unavailable");
		server.verify();
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
