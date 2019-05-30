package software.wings.integration.setup;

import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Workflow;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.setup.rest.WorkflowResourceRestClient;

public class WorkflowIntegrationTest extends BaseIntegrationTest {
  @Inject private WorkflowResourceRestClient workflowResourceRestClient;
  //  @Inject private WorkflowGenerator workflowGenerator;
  //  @Inject private OwnerManager ownerManager;

  @Test
  @Owner(emails = RAGHU)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldReturnSeedBasicWorkflow() {
    Workflow seedBasicWorkflow = workflowResourceRestClient.getSeedBasicWorkflow(client);
    assertThat(seedBasicWorkflow).isNotNull().hasFieldOrPropertyWithValue("name", SEED_BASIC_WORKFLOW_NAME);
  }

  @Test
  @Owner(emails = SRINIVAS)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldReturnSeedBuildWorkflow() {
    //    final Seed seed = new Seed(0);

    //    final Owners owners = ownerManager.create();

    //    Workflow seedBuildWorkflow = workflowGenerator.ensurePredefined(seed, owners, Workflows.BUILD_JENKINS);
    //    assertThat(seedBuildWorkflow).isNotNull().hasFieldOrPropertyWithValue("name", SEED_BUILD_WORKFLOW_NAME);
  }

  @Test
  @Owner(emails = RAGHU)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldReturnSeedRollingWorkflow() {
    Workflow seedRollingWorkflow = workflowResourceRestClient.getSeedRollingWorkflow(client);
    assertThat(seedRollingWorkflow).isNotNull().hasFieldOrPropertyWithValue("name", SEED_ROLLING_WORKFLOW_NAME);
  }
}
