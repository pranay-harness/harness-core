package software.wings.service.impl;

import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.service.intfc.InfrastructureDefinitionService;

public class InfrastructureDefinitionServiceTest extends WingsBaseTest {
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;

  @Test
  @Category(UnitTests.class)
  public void testGetDeploymentTypeCloudProviderOptions() {
    assertTrue(infrastructureDefinitionService.getDeploymentTypeCloudProviderOptions().size()
        == DeploymentType.values().length);
  }
}
