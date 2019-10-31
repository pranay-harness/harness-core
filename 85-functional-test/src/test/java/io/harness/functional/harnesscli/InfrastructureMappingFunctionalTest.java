package io.harness.functional.harnesscli;

import static io.harness.generator.InfrastructureMappingGenerator.InfrastructureMappings.K8S_ROLLING_TEST;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.InfrastructureMappingGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.InfrastructureMapping;

import java.io.IOException;
import java.util.List;

@Slf4j
public class InfrastructureMappingFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject HarnesscliHelper harnesscliHelper;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;

  private final Seed seed = new Seed(0);

  private InfrastructureMapping infrastructureMapping;
  private String appId;
  private String envId;
  private String serviceId;

  @Before
  public void setUp() throws IOException {
    Owners owners = ownerManager.create();
    infrastructureMapping = infrastructureMappingGenerator.ensurePredefined(seed, owners, K8S_ROLLING_TEST);
    appId = infrastructureMapping.getAppId();
    serviceId = infrastructureMapping.getServiceId();
    envId = infrastructureMapping.getEnvId();
    harnesscliHelper.loginToCLI();
  }

  @Test
  @Owner(emails = ROHIT)
  @Category(CliFunctionalTests.class)
  public void getInfraMappingsTest() throws IOException {
    String command =
        "harness get inframappings --application " + appId + " --environment " + envId + " --service " + serviceId;
    List<String> getInfraMappingOutput = harnesscliHelper.executeCLICommand(command);

    boolean newInfraMappingListed = false;
    for (String s : getInfraMappingOutput) {
      if (s.contains(infrastructureMapping.getUuid())) {
        newInfraMappingListed = true;
        break;
      }
    }
    assertThat(newInfraMappingListed).isTrue();
  }

  @Test
  @Owner(emails = ROHIT)
  @Category(CliFunctionalTests.class)
  public void getInfraMappingsTestWithWrongAppIdTest() throws IOException {
    String command = "harness get inframappings --application "
        + "wrongAppId"
        + " --environment " + envId + " --service " + serviceId;
    List<String> getInfraMappingOutput = harnesscliHelper.getCLICommandError(command);

    assertThat(getInfraMappingOutput.get(0).contains("User not authorized")).isTrue();
  }

  @Test
  @Owner(emails = ROHIT)
  @Category(CliFunctionalTests.class)
  public void getInfraMappingsTestWithWrongEnvIdTest() throws IOException {
    String command = "harness get inframappings --application " + appId + " --environment "
        + "wrongEnvId"
        + " --service " + serviceId;
    List<String> getInfraMappingOutput = harnesscliHelper.getCLICommandError(command);

    assertThat(getInfraMappingOutput.get(0).contains("User not authorized")).isTrue();
  }

  @Test
  @Owner(emails = ROHIT)
  @Category(CliFunctionalTests.class)
  public void getInfraMappingsTestWithWrongServiceIdTest() throws IOException {
    String command = "harness get inframappings --application " + appId + " --environment " + envId + " --service "
        + "wrongServiceId";
    List<String> getInfraMappingOutput = harnesscliHelper.executeCLICommand(command);

    assertThat(getInfraMappingOutput.get(0).contains(
                   "No Infra Mappings were found in the Application Environment and Service provided"))
        .isTrue();
  }
}
