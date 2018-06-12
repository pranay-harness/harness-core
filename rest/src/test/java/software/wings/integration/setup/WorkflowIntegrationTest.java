package software.wings.integration.setup;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.Workflow;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.setup.rest.WorkflowResourceRestClient;

@Ignore // TODO: fix this test it is intermittent
public class WorkflowIntegrationTest extends BaseIntegrationTest {
  @Inject private WorkflowResourceRestClient workflowResourceRestClient;

  @Test
  public void shouldReturnSeedBasicWorkflow() {
    Workflow seedBasicWorkflow = workflowResourceRestClient.getSeedBasicWorkflow(client);
    assertThat(seedBasicWorkflow).isNotNull().hasFieldOrPropertyWithValue("name", SEED_BASIC_WORKFLOW_NAME);
  }

  @Test
  public void shouldReturnSeedRollingWorkflow() {
    Workflow seedRollingWorkflow = workflowResourceRestClient.getSeedRollingWorkflow(client);
    assertThat(seedRollingWorkflow).isNotNull().hasFieldOrPropertyWithValue("name", SEED_ROLLING_WORKFLOW_NAME);
  }
}
