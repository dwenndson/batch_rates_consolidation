package batch_rates_consolidation;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class DiegoWenndsonApplicationTests {

	@Autowired
	private JobRepository jobRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void configuresJobRepository() {
		assertNotNull(jobRepository);
	}
}
