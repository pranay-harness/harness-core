package software.wings.integration.setup;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Workflow;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.setup.rest.WorkflowResourceRestClient;

@Ignore
public class WorkflowIntegrationTest extends BaseIntegrationTest {
  @Inject private WorkflowResourceRestClient workflowResourceRestClient;
  //  @Inject private WorkflowGenerator workflowGenerator;
  //  @Inject private OwnerManager ownerManager;

  @Test
  @Category(IntegrationTests.class)
  @Ignore
  public void shouldReturnSeedBasicWorkflow() {
    Workflow seedBasicWorkflow = workflowResourceRestClient.getSeedBasicWorkflow(client);
    assertThat(seedBasicWorkflow).isNotNull().hasFieldOrPropertyWithValue("name", SEED_BASIC_WORKFLOW_NAME);
  }

  @Test
  @Category(IntegrationTests.class)
  public void shouldReturnSeedBuildWorkflow() {
    //    final Seed seed = new Seed(0);

    //    final Owners owners = ownerManager.create();

    //    Workflow seedBuildWorkflow = workflowGenerator.ensurePredefined(seed, owners, Workflows.BUILD_JENKINS);
    //    assertThat(seedBuildWorkflow).isNotNull().hasFieldOrPropertyWithValue("name", SEED_BUILD_WORKFLOW_NAME);
  }

  @Test
  @Category(IntegrationTests.class)
  @Ignore
  public void shouldReturnSeedRollingWorkflow() {
    Workflow seedRollingWorkflow = workflowResourceRestClient.getSeedRollingWorkflow(client);
    assertThat(seedRollingWorkflow).isNotNull().hasFieldOrPropertyWithValue("name", SEED_ROLLING_WORKFLOW_NAME);
  }
}
