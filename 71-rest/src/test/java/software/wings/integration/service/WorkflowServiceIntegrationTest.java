package software.wings.integration.service;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.generator.OwnerManager;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.Randomizer;
import software.wings.generator.Randomizer.Seed;
import software.wings.generator.WorkflowGenerator;
import software.wings.generator.WorkflowGenerator.Workflows;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.setup.rest.WorkflowResourceRestClient;
import software.wings.rules.Integration;

@Integration
public class WorkflowServiceIntegrationTest extends BaseIntegrationTest {
  @Inject private OwnerManager ownerManager;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowResourceRestClient workflowResourceRestClient;

  @Test
  public void testTerraformWorkflow() {
    final Seed seed = Randomizer.seed();
    final Owners owners = ownerManager.create();
    workflowGenerator.ensurePredefined(seed, owners, Workflows.TERRAFORM);

    // TODO: execute
  }
}
