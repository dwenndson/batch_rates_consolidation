package batch_rates_consolidation.worker.service;

import batch_rates_consolidation.domain.TransacaoPix;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class HttpFeeExemptionService implements FeeExemptionService {

	private final RestClient restClient;
	private final String path;

	public HttpFeeExemptionService(RestClient restClient, String path) {
		this.restClient = restClient;
		this.path = path;
	}

	@Override
	public boolean isExempt(TransacaoPix transacaoPix) {
		try {
			ExemptionResponse response = restClient.get()
					.uri(path, transacaoPix.getEndToEndId())
					.retrieve()
					.body(ExemptionResponse.class);
			return response != null && response.exempt();
		}
		catch (RestClientResponseException exception) {
			if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
				return false;
			}
			if (exception.getStatusCode().is4xxClientError()) {
				throw new FeeExemptionClientException("Exemption service rejected the request");
			}
			throw new FeeExemptionException("Exemption service unavailable", exception);
		}
		catch (RuntimeException exception) {
			throw new FeeExemptionException("Exemption service unavailable", exception);
		}
	}
}
