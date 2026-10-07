package batch_rates_consolidation.manager.api;

import batch_rates_consolidation.manager.application.BatchExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/batches/pix-consolidation")
public class BatchExecutionController {

	private final BatchExecutionService batchExecutionService;

	public BatchExecutionController(BatchExecutionService batchExecutionService) {
		this.batchExecutionService = batchExecutionService;
	}

	@PostMapping("/executions")
	public ResponseEntity<BatchExecutionResponse> start(
			@Valid @RequestBody BatchExecutionRequest request) {
		BatchExecutionResponse response = batchExecutionService.start(request);
		return ResponseEntity.accepted()
				.location(URI.create("/api/v1/batches/pix-consolidation/executions/"
						+ response.jobExecutionId()))
				.body(response);
	}
}
