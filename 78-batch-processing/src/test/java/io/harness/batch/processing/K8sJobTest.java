package io.harness.batch.processing;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.IntegrationTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import software.wings.integration.BaseIntegrationTest;

import java.time.Instant;

@ActiveProfiles("test")
@ContextConfiguration(classes = BatchProcessingApplication.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class K8sJobTest extends BaseIntegrationTest {
  @Autowired private HPersistence hPersistence;
  @Autowired private JobLauncher jobLauncher;
  @Autowired @Qualifier(value = "k8sJob") private Job job;

  @Test
  @Owner(developers = HANTANG)
  @Category(IntegrationTests.class)
  public void testK8sJob() throws Exception {
    Instant startInstant = Instant.now();
    Instant endInstant = Instant.now().plusSeconds(60);

    JobParameters params = new JobParametersBuilder()
                               .addString("JobID", String.valueOf(System.currentTimeMillis()))
                               .addString("startDate", String.valueOf(startInstant.toEpochMilli()))
                               .addString("endDate", String.valueOf(endInstant.toEpochMilli()))
                               .toJobParameters();

    JobExecution jobExecution = jobLauncher.run(job, params);
    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
  }
}
