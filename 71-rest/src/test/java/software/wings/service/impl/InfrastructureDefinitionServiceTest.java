package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.LaunchType;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsInstanceFilter.AwsInstanceFilterKeys;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsAmiInfrastructure.AwsAmiInfrastructureKeys;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AwsEcsInfrastructure.AwsEcsInfrastructureKeys;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure.AwsInstanceInfrastructureKeys;
import software.wings.infra.AwsLambdaInfrastructure;
import software.wings.infra.AwsLambdaInfrastructure.AwsLambdaInfrastructureKeys;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.GoogleKubernetesEngine.GoogleKubernetesEngineKeys;
import software.wings.infra.PhysicalInfra;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureDefinitionServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class InfrastructureDefinitionServiceTest extends WingsBaseTest {
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;

  @Test
  @Category(UnitTests.class)
  public void testGetDeploymentTypeCloudProviderOptions() {
    assertTrue(infrastructureDefinitionService.getDeploymentTypeCloudProviderOptions().size()
        == DeploymentType.values().length);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateAwsEcsInfraWithProvisioner() {
    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
    String randomValue = "val";

    AwsEcsInfrastructure infra = AwsEcsInfrastructure.builder().build();
    Map<String, String> expressions = new HashMap<>();
    infra.setExpressions(expressions);
    assertThatThrownBy(() -> infrastructureDefinitionService.validateAwsEcsInfraWithProvisioner(infra));

    infra.setLaunchType(LaunchType.EC2.toString());
    expressions.put(AwsEcsInfrastructureKeys.region, randomValue);
    expressions.put(AwsEcsInfrastructureKeys.clusterName, randomValue);
    infrastructureDefinitionService.validateAwsEcsInfraWithProvisioner(infra);

    infra.setLaunchType(LaunchType.FARGATE.toString());
    assertThatThrownBy(() -> infrastructureDefinitionService.validateAwsEcsInfraWithProvisioner(infra));
    expressions.put(AwsEcsInfrastructureKeys.executionRole, randomValue);
    expressions.put(AwsEcsInfrastructureKeys.subnetIds, randomValue);
    expressions.put(AwsEcsInfrastructureKeys.securityGroupIds, randomValue);
    expressions.put(AwsEcsInfrastructureKeys.vpcId, randomValue);
    infrastructureDefinitionService.validateAwsEcsInfraWithProvisioner(infra);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidatePhysicalInfraWithProvisioner() {
    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
    String randomValue = "val";

    PhysicalInfra infra = PhysicalInfra.builder().build();
    Map<String, String> expressions = new HashMap<>();
    infra.setExpressions(expressions);
    assertThatThrownBy(() -> infrastructureDefinitionService.validatePhysicalInfraWithProvisioner(infra));

    expressions.put(PhysicalInfra.hostname, randomValue);
    expressions.put(PhysicalInfra.hostArrayPath, randomValue);
    infrastructureDefinitionService.validatePhysicalInfraWithProvisioner(infra);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateGoogleKubernetesEngineInfraWithProvisioner() {
    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
    String randomValue = "val";

    GoogleKubernetesEngine infra = GoogleKubernetesEngine.builder().build();
    Map<String, String> expressions = new HashMap<>();
    infra.setExpressions(expressions);
    assertThatThrownBy(() -> infrastructureDefinitionService.validateGoogleKubernetesEngineInfraWithProvisioner(infra));

    expressions.put(GoogleKubernetesEngineKeys.clusterName, randomValue);
    expressions.put(GoogleKubernetesEngineKeys.namespace, randomValue);
    infrastructureDefinitionService.validateGoogleKubernetesEngineInfraWithProvisioner(infra);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateAwsLambdaInfraWithProvisioner() {
    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
    String randomValue = "val";

    AwsLambdaInfrastructure infra = AwsLambdaInfrastructure.builder().build();
    Map<String, String> expressions = new HashMap<>();
    infra.setExpressions(expressions);
    assertThatThrownBy(() -> infrastructureDefinitionService.validateAwsLambdaInfraWithProvisioner(infra));

    expressions.put(AwsLambdaInfrastructureKeys.region, randomValue);
    expressions.put(AwsLambdaInfrastructureKeys.role, randomValue);
    infrastructureDefinitionService.validateAwsLambdaInfraWithProvisioner(infra);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateAwsAmiInfraWithProvisioner() {
    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
    String randomValue = "val";

    AwsAmiInfrastructure infra = AwsAmiInfrastructure.builder().build();
    Map<String, String> expressions = new HashMap<>();
    infra.setExpressions(expressions);
    assertThatThrownBy(() -> infrastructureDefinitionService.validateAwsAmiInfraWithProvisioner(infra));

    expressions.put(AwsAmiInfrastructureKeys.region, randomValue);
    expressions.put(AwsAmiInfrastructureKeys.autoScalingGroupName, randomValue);
    infrastructureDefinitionService.validateAwsAmiInfraWithProvisioner(infra);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateAwsInstanceInfraWithProvisioner() {
    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
    String randomValue = "val";

    AwsInstanceInfrastructure infra = AwsInstanceInfrastructure.builder().build();
    Map<String, String> expressions = new HashMap<>();
    infra.setExpressions(expressions);
    assertThatThrownBy(() -> infrastructureDefinitionService.validateAwsInstanceInfraWithProvisioner(infra));

    expressions.put(AwsInstanceInfrastructureKeys.region, randomValue);
    expressions.put(AwsInstanceFilterKeys.tags, randomValue);
    infrastructureDefinitionService.validateAwsInstanceInfraWithProvisioner(infra);

    infra.setProvisionInstances(true);
    infra.setDesiredCapacity(1);
    assertThatThrownBy(() -> infrastructureDefinitionService.validateAwsInstanceInfraWithProvisioner(infra));
    expressions.put(AwsInstanceInfrastructureKeys.autoScalingGroupName, randomValue);
    infrastructureDefinitionService.validateAwsInstanceInfraWithProvisioner(infra);
  }

  @Test
  @Category(UnitTests.class)
  public void testRemoveUnsupportedExpressions() {
    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
    AwsInstanceInfrastructure awsInstanceInfrastructure = AwsInstanceInfrastructure.builder().build();
    Map<String, String> expressions = new HashMap<>();
    expressions.put(AwsInstanceInfrastructureKeys.region, "randomValue");
    expressions.put("randomKey", "randomValue");
    awsInstanceInfrastructure.setExpressions(expressions);
    infrastructureDefinitionService.removeUnsupportedExpressions(awsInstanceInfrastructure);

    Map<String, String> expectedExpressions = new HashMap<>();
    expectedExpressions.put(AwsInstanceInfrastructureKeys.region, "randomValue");
    assertTrue(expectedExpressions.equals(awsInstanceInfrastructure.getExpressions()));
  }
}
