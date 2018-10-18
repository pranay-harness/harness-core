package software.wings.integration.service;

import software.wings.integration.BaseIntegrationTest;
import software.wings.rules.Integration;

@Integration
public class WorkflowServiceIntegrationTest extends BaseIntegrationTest {
  /*@Inject private OwnerManager ownerManager;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowResourceRestClient workflowResourceRestClient;

  @Test
  public void testTerraformWorkflow() {
    final Seed seed = Randomizer.seed();
    final Owners owners = ownerManager.create();
    workflowGenerator.ensurePredefined(seed, owners, Workflows.TERRAFORM);

    // TODO: execute
  }*/
}
