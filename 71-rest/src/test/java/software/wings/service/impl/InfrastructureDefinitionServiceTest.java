package software.wings.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.infra.InfraDefinitionTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.LaunchType;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsInstanceFilter.AwsInstanceFilterKeys;
import software.wings.dl.WingsPersistence;
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
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.PhysicalInfra;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureDefinitionServiceImpl;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InfrastructureDefinitionServiceTest extends WingsBaseTest {
  @Mock private Query<InfrastructureDefinition> query;
  @Mock private WingsPersistence wingsPersistence;

  @Mock private WorkflowService workflowService;
  @Mock private PipelineService pipelineService;
  @Mock private TriggerService triggerService;

  @Inject @InjectMocks private InfrastructureDefinitionService infrastructureDefinitionService;

  @Spy
  @InjectMocks
  private InfrastructureDefinitionService spyInfrastructureDefinitionService =
      new InfrastructureDefinitionServiceImpl();

  @Test
  @Category(UnitTests.class)
  public void testGetDeploymentTypeCloudProviderOptions() {
    assertThat(infrastructureDefinitionService.getDeploymentTypeCloudProviderOptions().size()
        == DeploymentType.values().length)
        .isTrue();
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
    assertEquals(expectedExpressions, awsInstanceInfrastructure.getExpressions());
  }

  @Test
  @Category(UnitTests.class)
  public void testCloneAndSaveWith() {
    // given
    InfrastructureDefinition infraDef =
        InfrastructureDefinition.builder().uuid("infra-uuid").envId("envid").appId("appid").name("infra-name").build();

    doReturn(InfrastructureDefinition.builder().build())
        .when(spyInfrastructureDefinitionService)
        .save(any(InfrastructureDefinition.class), any(boolean.class));
    doReturn(aPageResponse().withResponse(Collections.singletonList(infraDef)).build())
        .when(spyInfrastructureDefinitionService)
        .list("appid", "envid", null);

    spyInfrastructureDefinitionService.cloneInfrastructureDefinitions("appid", "envid", "appid-1", "envid-1");

    // test
    ArgumentCaptor<InfrastructureDefinition> cloneInfraDef = ArgumentCaptor.forClass(InfrastructureDefinition.class);

    verify(spyInfrastructureDefinitionService, times(1)).save(cloneInfraDef.capture(), any(boolean.class));
    verify(spyInfrastructureDefinitionService, times(1)).list("appid", "envid", null);

    InfrastructureDefinition value = cloneInfraDef.getValue();
    assertThat(value.getUuid()).isNull();
    assertEquals(value.getEnvId(), "envid-1");
    assertEquals(value.getAppId(), "appid-1");
    assertEquals(value.getName(), "infra-name");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldThrowExceptionOnDeleteReferencedByWorkflow() {
    mockPhysicalInfra();
    when(workflowService.obtainWorkflowNamesReferencedByInfrastructureDefinition(APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(asList("Referenced Workflow"));

    Assertions.assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> infrastructureDefinitionService.delete(APP_ID, INFRA_DEFINITION_ID));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldThrowExceptionOnDeleteReferencedByTrigger() {
    mockPhysicalInfra();
    when(workflowService.obtainWorkflowNamesReferencedByInfrastructureDefinition(APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(asList());
    when(pipelineService.obtainPipelineNamesReferencedByTemplatedEntity(APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(asList());

    when(triggerService.obtainTriggerNamesReferencedByTemplatedEntityId(APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(asList("Referenced Trigger"));
    assertThatThrownBy(() -> infrastructureDefinitionService.delete(APP_ID, INFRA_DEFINITION_ID))
        .isInstanceOf(WingsException.class);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldThrowExceptionOnDeleteReferencedByPipeline() {
    mockPhysicalInfra();
    when(workflowService.obtainWorkflowNamesReferencedByServiceInfrastructure(APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(asList());
    when(pipelineService.obtainPipelineNamesReferencedByTemplatedEntity(APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(asList("Referenced Pipeline"));
    assertThatThrownBy(() -> infrastructureDefinitionService.delete(APP_ID, INFRA_DEFINITION_ID))
        .isInstanceOf(WingsException.class);
  }

  private void mockPhysicalInfra() {
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .uuid(INFRA_DEFINITION_ID)
            .appId(APP_ID)
            .envId(ENV_ID)
            .cloudProviderType(CloudProviderType.PHYSICAL_DATA_CENTER)
            .deploymentType(DeploymentType.SSH)
            .infrastructure(
                PhysicalInfra.builder().cloudProviderId(SETTING_ID).hostNames(singletonList(HOST_NAME)).build())
            .build();

    when(wingsPersistence.getWithAppId(InfrastructureDefinition.class, APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(infrastructureDefinition);
  }
}
