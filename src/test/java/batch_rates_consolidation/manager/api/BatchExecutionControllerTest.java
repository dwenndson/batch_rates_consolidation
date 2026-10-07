package batch_rates_consolidation.manager.api;

import batch_rates_consolidation.manager.application.BatchExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchExecutionController.class)
@Import(batch_rates_consolidation.config.SecurityConfig.class)
class BatchExecutionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BatchExecutionService batchExecutionService;

	@Test
	void startsBatchExecution() throws Exception {
		when(batchExecutionService.start(new BatchExecutionRequest(10_000)))
				.thenReturn(new BatchExecutionResponse(42L, "STARTING"));

		mockMvc.perform(post("/api/v1/batches/pix-consolidation/executions")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"partitionSize":10000}
								"""))
				.andExpect(status().isAccepted())
				.andExpect(header().string("Location",
						"/api/v1/batches/pix-consolidation/executions/42"))
				.andExpect(jsonPath("$.jobExecutionId").value(42))
				.andExpect(jsonPath("$.status").value("STARTING"));

		verify(batchExecutionService).start(new BatchExecutionRequest(10_000));
	}

	@Test
	void rejectsInvalidPartitionSize() throws Exception {
		mockMvc.perform(post("/api/v1/batches/pix-consolidation/executions")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"partitionSize":0}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsUnauthenticatedRequest() throws Exception {
		mockMvc.perform(post("/api/v1/batches/pix-consolidation/executions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"partitionSize":10000}
								"""))
				.andExpect(status().isUnauthorized());
	}
}
