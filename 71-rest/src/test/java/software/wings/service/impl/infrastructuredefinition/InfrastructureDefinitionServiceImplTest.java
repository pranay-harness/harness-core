package software.wings.service.impl.infrastructuredefinition;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.DINESH;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.RIHAZ;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.InfrastructureType.AWS_ECS;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA;
import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA_WINRM;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.common.InfrastructureConstants.INFRA_KUBERNETES_INFRAID_EXPRESSION;
import static software.wings.infra.InfraDefinitionTestConstants.INFRA_DEFINITION_ID;
import static software.wings.infra.InfraDefinitionTestConstants.INFRA_DEFINITION_NAME;
import static software.wings.infra.InfraDefinitionTestConstants.INFRA_PROVISIONER_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import com.amazonaws.services.ecs.model.LaunchType;
import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInstanceFilter.AwsInstanceFilterKeys;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.dl.WingsPersistence;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsAmiInfrastructure.AwsAmiInfrastructureKeys;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AwsEcsInfrastructure.AwsEcsInfrastructureKeys;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure.AwsInstanceInfrastructureKeys;
import software.wings.infra.AwsLambdaInfrastructure;
import software.wings.infra.AwsLambdaInfrastructure.AwsLambdaInfrastructureKeys;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.GoogleKubernetesEngine.GoogleKubernetesEngineKeys;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionBuilder;
import software.wings.infra.PcfInfraStructure;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.ExecutionContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.UriInfo;

public class InfrastructureDefinitionServiceImplTest extends WingsBaseTest {
  @Mock private ExecutionContext executionContext;
  @Mock private WorkflowService workflowService;
  @Mock private PipelineService pipelineService;
  @Mock private TriggerService triggerService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AppService appService;
  @Mock private SettingsService mockSettingsService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private YamlPushService yamlPushService;
  @Mock private EventPublishHelper eventPublishHelper;

  @Spy @InjectMocks private InfrastructureDefinitionServiceImpl infrastructureDefinitionService;

  private static final String DEFAULT = "default";
  private static final String USER_INPUT_NAMESPACE = "USER_INPUT_NAMESPACE";

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldFailOnNonResolutionOfExpressions() {
    String wrongVariable = "${WRONG_VARIABLE}";
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().namespace(wrongVariable).build())
            .build();
    when(executionContext.renderExpression(wrongVariable)).thenReturn(wrongVariable);

    assertThatThrownBy(
        () -> infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(wrongVariable);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldFailOnNullRenderedValueOfExpression() {
    String wrongVariable = "${WRONG_VARIABLE}";
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().namespace(wrongVariable).build())
            .build();
    when(executionContext.renderExpression(wrongVariable)).thenReturn(null);

    assertThatThrownBy(
        () -> infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(wrongVariable);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldFailOnStringNullRenderedValueOfExpression() {
    String wrongVariable = "${WRONG_VARIABLE}";
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().namespace(wrongVariable).build())
            .build();
    when(executionContext.renderExpression(wrongVariable)).thenReturn("null");

    assertThatThrownBy(
        () -> infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(wrongVariable);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldResolveExpressions() {
    String workflowVariable = "abc-${workflow.variables.var}";
    String value = "value";
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().namespace(workflowVariable).build())
            .build();
    when(executionContext.renderExpression(workflowVariable)).thenReturn(value);

    infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext);

    assertThat(((GoogleKubernetesEngine) infrastructureDefinition.getInfrastructure()).getNamespace()).isEqualTo(value);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldIgnoreReleaseNameResolutionFailure() {
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().releaseName(INFRA_KUBERNETES_INFRAID_EXPRESSION).build())
            .build();
    when(executionContext.renderExpression(INFRA_KUBERNETES_INFRAID_EXPRESSION))
        .thenReturn(INFRA_KUBERNETES_INFRAID_EXPRESSION);

    infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext);

    assertThat(((GoogleKubernetesEngine) infrastructureDefinition.getInfrastructure()).getReleaseName())
        .isEqualTo(INFRA_KUBERNETES_INFRAID_EXPRESSION);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldIgnoreReleaseNameWithSomeConstantResolutionFailure() {
    String releaseName = "release-" + INFRA_KUBERNETES_INFRAID_EXPRESSION;
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().releaseName(releaseName).build())
            .build();
    when(executionContext.renderExpression(releaseName)).thenReturn(releaseName);

    infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext);

    assertThat(((GoogleKubernetesEngine) infrastructureDefinition.getInfrastructure()).getReleaseName())
        .isEqualTo(releaseName);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testEnsureSafeToDelete_error() {
    final InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder().uuid("infraid").name("infra_name").build();

    doReturn(singletonList("workflow1"))
        .when(workflowService)
        .obtainWorkflowNamesReferencedByInfrastructureDefinition(anyString(), anyString());
    infrastructureDefinitionService.ensureSafeToDelete("appid", infrastructureDefinition);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testEnsureSafeToDelete_error1() {
    final InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder().uuid("infraid").name("infra_name").build();

    doReturn(emptyList())
        .when(workflowService)
        .obtainWorkflowNamesReferencedByInfrastructureDefinition(anyString(), anyString());

    doReturn(singletonList("pipeline1"))
        .when(pipelineService)
        .obtainPipelineNamesReferencedByTemplatedEntity(anyString(), anyString());

    infrastructureDefinitionService.ensureSafeToDelete("appid", infrastructureDefinition);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testEnsureSafeToDelete_error2() {
    final InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder().uuid("infraid").name("infra_name").build();

    doReturn(emptyList())
        .when(workflowService)
        .obtainWorkflowNamesReferencedByInfrastructureDefinition(anyString(), anyString());

    doReturn(emptyList())
        .when(pipelineService)
        .obtainPipelineNamesReferencedByTemplatedEntity(anyString(), anyString());

    doReturn(singletonList("triggerid"))
        .when(triggerService)
        .obtainTriggerNamesReferencedByTemplatedEntityId(anyString(), anyString());

    infrastructureDefinitionService.ensureSafeToDelete("appid", infrastructureDefinition);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGetDeploymentTypeCloudProviderOptions() {
    assertThat(infrastructureDefinitionService.getDeploymentTypeCloudProviderOptions().size()
        == DeploymentType.values().length)
        .isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateAwsEcsInfraWithProvisioner() {
    //    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
    //        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
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
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidatePhysicalInfraWithProvisioner() {
    //    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
    //        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
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
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateGoogleKubernetesEngineInfraWithProvisioner() {
    //    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
    //        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
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
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateAwsLambdaInfraWithProvisioner() {
    //    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
    //        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
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
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateAwsAmiInfraWithProvisioner() {
    //    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
    //        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
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
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateAwsInstanceInfraWithProvisioner() {
    //    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
    //        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
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
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testRemoveUnsupportedExpressions() {
    //    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
    //        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
    AwsInstanceInfrastructure awsInstanceInfrastructure = AwsInstanceInfrastructure.builder().build();
    Map<String, String> expressions = new HashMap<>();
    expressions.put(AwsInstanceInfrastructureKeys.region, "randomValue");
    expressions.put("randomKey", "randomValue");
    awsInstanceInfrastructure.setExpressions(expressions);
    infrastructureDefinitionService.removeUnsupportedExpressions(awsInstanceInfrastructure);

    Map<String, String> expectedExpressions = new HashMap<>();
    expectedExpressions.put(AwsInstanceInfrastructureKeys.region, "randomValue");
    assertThat(awsInstanceInfrastructure.getExpressions()).isEqualTo(expectedExpressions);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testCloneAndSaveWith() {
    InfrastructureDefinitionService spyInfrastructureDefinitionService = spy(InfrastructureDefinitionServiceImpl.class);
    InfrastructureDefinition infraDef =
        InfrastructureDefinition.builder().uuid("infra-uuid").envId("envid").appId("appid").name("infra-name").build();

    doReturn(InfrastructureDefinition.builder().build())
        .when(spyInfrastructureDefinitionService)
        .save(any(InfrastructureDefinition.class), any(boolean.class), any(boolean.class));
    doReturn(aPageResponse().withResponse(Collections.singletonList(infraDef)).build())
        .when(spyInfrastructureDefinitionService)
        .list("appid", "envid", null);

    spyInfrastructureDefinitionService.cloneInfrastructureDefinitions("appid", "envid", "appid-1", "envid-1");

    // test
    ArgumentCaptor<InfrastructureDefinition> cloneInfraDef = ArgumentCaptor.forClass(InfrastructureDefinition.class);

    verify(spyInfrastructureDefinitionService, times(1))
        .save(cloneInfraDef.capture(), any(boolean.class), any(boolean.class));
    verify(spyInfrastructureDefinitionService, times(1)).list("appid", "envid", null);

    InfrastructureDefinition value = cloneInfraDef.getValue();
    assertThat(value.getUuid()).isNull();
    assertThat("envid-1").isEqualTo(value.getEnvId());
    assertThat("appid-1").isEqualTo(value.getAppId());
    assertThat("infra-name").isEqualTo(value.getName());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldThrowExceptionOnDeleteReferencedByWorkflow() {
    mockPhysicalInfra();
    when(workflowService.obtainWorkflowNamesReferencedByInfrastructureDefinition(APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(asList("Referenced Workflow"));

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> infrastructureDefinitionService.delete(APP_ID, INFRA_DEFINITION_ID));
  }

  @Test
  @Owner(developers = PRASHANT)
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
  @Owner(developers = PRASHANT)
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
    InfrastructureDefinition infrastructureDefinition = getValidInfra(PHYSICAL_INFRA, false);

    when(wingsPersistence.getWithAppId(InfrastructureDefinition.class, APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(infrastructureDefinition);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetExpressionAnnotatedFields() {
    InfrastructureDefinitionServiceImpl infrastructureDefinitionService = this.infrastructureDefinitionService;
    GoogleKubernetesEngine googleKubernetesEngine = GoogleKubernetesEngine.builder().namespace("namespace").build();

    Map<String, Object> allFields =
        infrastructureDefinitionService.getExpressionAnnotatedFields(googleKubernetesEngine);

    assertThat(allFields).hasSize(1);
    assertThat(allFields.get(GoogleKubernetesEngineKeys.namespace).equals("namespace")).isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnEmptyExpressionAnnotatedFields() {
    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
        (InfrastructureDefinitionServiceImpl) this.infrastructureDefinitionService;
    AwsInstanceInfrastructure awsInstanceInfra = AwsInstanceInfrastructure.builder().build();

    Map<String, Object> allFields = infrastructureDefinitionService.getExpressionAnnotatedFields(awsInstanceInfra);

    assertThat(allFields).isEmpty();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testApplySearchFilter() {
    UriInfo uriInfo = mock(UriInfo.class);
    Map<String, List<String>> queryParams = new HashMap<>();
    when(uriInfo.getQueryParameters()).thenReturn(new AbstractMultivaluedMap<String, String>(queryParams) {});
    PageRequest<InfrastructureDefinition> pageRequest = new PageRequest<>();
    pageRequest.setUriInfo(uriInfo);

    assertThatThrownBy(() -> infrastructureDefinitionService.applyServiceFilter(pageRequest));

    queryParams.put("serviceId", Collections.singletonList("s1"));
    queryParams.put("appId", Collections.singletonList("app1"));
    Service service = Service.builder().deploymentType(DeploymentType.SSH).build();
    when(serviceResourceService.get(anyString(), anyString())).thenReturn(service);
    infrastructureDefinitionService.applyServiceFilter(pageRequest);
    assertThat(pageRequest.getFilters().size() == 2).isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateImmutableFields() {
    InfrastructureDefinition oldInfraDefinition = InfrastructureDefinition.builder()
                                                      .deploymentType(DeploymentType.SSH)
                                                      .cloudProviderType(CloudProviderType.AWS)
                                                      .build();
    InfrastructureDefinition newInfraDefinition = InfrastructureDefinition.builder()
                                                      .deploymentType(DeploymentType.SSH)
                                                      .cloudProviderType(CloudProviderType.AWS)
                                                      .build();

    infrastructureDefinitionService.validateImmutableFields(newInfraDefinition, oldInfraDefinition);

    newInfraDefinition.setDeploymentType(DeploymentType.HELM);
    assertThatThrownBy(
        () -> infrastructureDefinitionService.validateImmutableFields(newInfraDefinition, oldInfraDefinition));
    newInfraDefinition.setDeploymentType(oldInfraDefinition.getDeploymentType());

    newInfraDefinition.setCloudProviderType(CloudProviderType.GCP);
    assertThatThrownBy(
        () -> infrastructureDefinitionService.validateImmutableFields(newInfraDefinition, oldInfraDefinition));
    newInfraDefinition.setCloudProviderType(oldInfraDefinition.getCloudProviderType());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testValidateInfraDefinition() {
    InfrastructureDefinition valid = null;
    valid = getValidInfra(PHYSICAL_INFRA, false);
    infrastructureDefinitionService.validateInfraDefinition(valid);
    verify(infrastructureMappingService, times(1)).validateInfraMapping(valid.getInfraMapping(), false);
    InfrastructureDefinition inValid_phy = valid.cloneForUpdate();
    inValid_phy.setDeploymentType(DeploymentType.HELM);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> infrastructureDefinitionService.validateInfraDefinition(inValid_phy));

    valid = getValidInfra(PHYSICAL_INFRA, true);
    InfrastructureDefinition inValid_phy_prov = valid.cloneForUpdate();
    for (String key : ImmutableList.of(PhysicalInfra.hostname, PhysicalInfra.hostArrayPath)) {
      ((PhysicalInfra) inValid_phy_prov.getInfrastructure()).getExpressions().put(key, EMPTY);
      assertThatExceptionOfType(InvalidRequestException.class)
          .isThrownBy(() -> infrastructureDefinitionService.validateInfraDefinition(inValid_phy_prov));
      ((PhysicalInfra) inValid_phy_prov.getInfrastructure()).getExpressions().put(key, "default");
    }

    valid = getValidInfra(PHYSICAL_INFRA_WINRM, false);
    infrastructureDefinitionService.validateInfraDefinition(valid);
    verify(infrastructureMappingService, times(1)).validateInfraMapping(valid.getInfraMapping(), false);
    InfrastructureDefinition inValid_phy_winrm = valid.cloneForUpdate();
    inValid_phy_winrm.setDeploymentType(DeploymentType.HELM);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> infrastructureDefinitionService.validateInfraDefinition(inValid_phy_winrm));

    valid = getValidInfra(GCP_KUBERNETES_ENGINE, true);
    infrastructureDefinitionService.validateInfraDefinition(valid);
    verify(infrastructureMappingService, times(1)).validateInfraMapping(valid.getInfraMapping(), false);
    InfrastructureDefinition inValid_gcpK8s = valid.cloneForUpdate();
    InfrastructureDefinition invalid_gcp_k8s_prov = valid.cloneForUpdate();
    for (String key : ImmutableList.of(GoogleKubernetesEngineKeys.clusterName, GoogleKubernetesEngineKeys.namespace)) {
      ((GoogleKubernetesEngine) invalid_gcp_k8s_prov.getInfrastructure()).getExpressions().put(key, EMPTY);
      assertThatExceptionOfType(InvalidRequestException.class)
          .isThrownBy(() -> infrastructureDefinitionService.validateInfraDefinition(invalid_gcp_k8s_prov));
      ((GoogleKubernetesEngine) invalid_gcp_k8s_prov.getInfrastructure()).getExpressions().put(key, "default");
    }

    valid = getValidInfra(AWS_ECS, true);
    infrastructureDefinitionService.validateInfraDefinition(valid);
    verify(infrastructureMappingService, times(1)).validateInfraMapping(valid.getInfraMapping(), false);
    InfrastructureDefinition invalid_awsEcs = valid.cloneForUpdate();
    for (String key : ImmutableList.<String>of(AwsEcsInfrastructureKeys.region, AwsEcsInfrastructureKeys.clusterName,
             AwsEcsInfrastructureKeys.executionRole, AwsEcsInfrastructureKeys.vpcId,
             AwsEcsInfrastructureKeys.securityGroupIds, AwsEcsInfrastructureKeys.subnetIds)) {
      ((AwsEcsInfrastructure) invalid_awsEcs.getInfrastructure()).getExpressions().put(key, EMPTY);
      assertThatExceptionOfType(InvalidRequestException.class)
          .isThrownBy(() -> infrastructureDefinitionService.validateInfraDefinition(invalid_awsEcs));
      ((AwsEcsInfrastructure) invalid_awsEcs.getInfrastructure()).getExpressions().put(key, "default");
    }
    ((AwsEcsInfrastructure) invalid_awsEcs.getInfrastructure()).setLaunchType(EMPTY);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> infrastructureDefinitionService.validateInfraDefinition(invalid_awsEcs));
  }

  private InfrastructureDefinition getValidInfra(@NotNull String type, boolean withProvisioner) {
    InfrastructureDefinitionBuilder builder = InfrastructureDefinition.builder()
                                                  .name(INFRA_DEFINITION_NAME)
                                                  .uuid(INFRA_DEFINITION_ID)
                                                  .appId(APP_ID)
                                                  .envId(ENV_ID);

    List<String> hosts = ImmutableList.of(HOST_NAME);
    HashMap<String, String> expMap = Maps.<String, String>newHashMap();
    switch (type) {
      case PHYSICAL_INFRA:
        PhysicalInfra infra = null;
        if (withProvisioner) {
          expMap.put(PhysicalInfra.hostArrayPath, "host");
          expMap.put(PhysicalInfra.hostname, "hostname");
          infra = PhysicalInfra.builder()
                      .hostConnectionAttrs("dev-key")
                      .cloudProviderId(SETTING_ID)
                      .expressions(expMap)
                      .build();
          builder.provisionerId(INFRA_PROVISIONER_ID);
        } else {
          infra = PhysicalInfra.builder()
                      .hostConnectionAttrs("dev-key")
                      .cloudProviderId(SETTING_ID)
                      .hostNames(hosts)
                      .build();
        }
        return builder.cloudProviderType(CloudProviderType.PHYSICAL_DATA_CENTER)
            .deploymentType(DeploymentType.SSH)
            .infrastructure(infra)
            .build();
      case PHYSICAL_INFRA_WINRM:
        if (withProvisioner) {
          return null;
        } else {
          return builder.cloudProviderType(CloudProviderType.PHYSICAL_DATA_CENTER)
              .deploymentType(DeploymentType.WINRM)
              .infrastructure(PhysicalInfraWinrm.builder()
                                  .winRmConnectionAttributes("test.key")
                                  .cloudProviderId(SETTING_ID)
                                  .hostNames(hosts)
                                  .build())
              .build();
        }
      case GCP_KUBERNETES_ENGINE:
        if (withProvisioner) {
          expMap.put("clusterName", "${terraform.cluster}");
          expMap.put("namespace", "${terraform.namespace}");
          return builder.provisionerId(INFRA_PROVISIONER_ID)
              .deploymentType(DeploymentType.KUBERNETES)
              .cloudProviderType(CloudProviderType.GCP)
              .infrastructure(GoogleKubernetesEngine.builder().expressions(expMap).build())
              .build();
        } else {
          return null;
        }
      case AWS_ECS:
        if (withProvisioner) {
          expMap.put(AwsEcsInfrastructureKeys.region, "region");
          expMap.put(AwsEcsInfrastructureKeys.clusterName, "cluster");
          expMap.put(AwsEcsInfrastructureKeys.vpcId, "vpc");
          expMap.put(AwsEcsInfrastructureKeys.securityGroupIds, "sgid");
          expMap.put(AwsEcsInfrastructureKeys.subnetIds, "sid");
          expMap.put(AwsEcsInfrastructureKeys.executionRole, "execRole");
          return builder.provisionerId(INFRA_PROVISIONER_ID)
              .deploymentType(DeploymentType.ECS)
              .cloudProviderType(CloudProviderType.AWS)
              .infrastructure(
                  AwsEcsInfrastructure.builder().launchType(LaunchType.FARGATE.name()).expressions(expMap).build())
              .build();
        } else {
          return builder.deploymentType(DeploymentType.ECS)
              .cloudProviderType(CloudProviderType.AWS)
              .infrastructure(AwsEcsInfrastructure.builder()
                                  .launchType(LaunchType.FARGATE.name())
                                  .region("us-east-1")
                                  .clusterName("test")
                                  .build())
              .build();
        }
      default:
        throw new InvalidRequestException("Invalid type");
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testContainsExpression() {
    assertThat(infrastructureDefinitionService.containsExpression("org")).isFalse();
    assertThat(infrastructureDefinitionService.containsExpression("${serviceVariable.val}")).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testListRoutesForPcf() throws Exception {
    InfrastructureDefinitionServiceImpl definitionService = spy(InfrastructureDefinitionServiceImpl.class);
    SettingsService settingsService = mock(SettingsService.class);
    FieldUtils.writeField(definitionService, "settingsService", settingsService, true);
    doReturn(null).when(definitionService).get(anyString(), anyString());

    assertThatThrownBy(() -> definitionService.listRoutesForPcf("app", "def"))
        .isNotInstanceOf(NullPointerException.class);

    doReturn(InfrastructureDefinition.builder().build()).when(definitionService).get(anyString(), anyString());
    assertThatThrownBy(() -> definitionService.listRoutesForPcf("app", "def"));

    doReturn(InfrastructureDefinition.builder().infrastructure(AwsAmiInfrastructure.builder().build()).build())
        .when(definitionService)
        .get(anyString(), anyString());
    assertThatThrownBy(() -> definitionService.listRoutesForPcf("app", "def"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Not PcfInfraStructure, invalid type");

    doReturn(InfrastructureDefinition.builder().infrastructure(PcfInfraStructure.builder().build()).build())
        .when(definitionService)
        .get(anyString(), anyString());
    doReturn(null).when(settingsService).get(anyString());
    assertThatThrownBy(() -> definitionService.listRoutesForPcf("app", "def"));

    doReturn(aSettingAttribute().withValue(AwsConfig.builder().build()).build()).when(settingsService).get(anyString());
    assertThatThrownBy(() -> definitionService.listRoutesForPcf("app", "def"))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testCloudProviderNameForDefinition() {
    SettingAttribute cloudProvider =
        aSettingAttribute()
            .withName("aws-cp")
            .withUuid("aws-cp-id")
            .withAccountId(ACCOUNT_ID)
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withValue(AwsConfig.builder().accessKey("access-key").secretKey("secret-key".toCharArray()).build())
            .build();

    InfrastructureDefinition def = getValidInfra(PHYSICAL_INFRA, false);
    ((PhysicalInfra) def.getInfrastructure()).setCloudProviderId(cloudProvider.getUuid());

    when(wingsPersistence.getWithAppId(InfrastructureDefinition.class, def.getAppId(), def.getUuid())).thenReturn(def);
    when(mockSettingsService.get(def.getInfrastructure().getCloudProviderId())).thenReturn(cloudProvider, null);

    assertThat(infrastructureDefinitionService.cloudProviderNameForDefinition(def.getAppId(), def.getUuid()))
        .isEqualTo(cloudProvider.getName());
    assertThat(infrastructureDefinitionService.cloudProviderNameForDefinition(def.getAppId(), def.getUuid()))
        .isEqualTo(null);
  }

  @Test
  @Owner(developers = DINESH)
  @Category(UnitTests.class)
  public void testSaveWithSkipValidation() {
    InfrastructureDefinition infraDef =
        InfrastructureDefinition.builder().uuid("infra-uuid").envId("envid").appId("appid").name("infra-name").build();

    when(wingsPersistence.save(any(InfrastructureDefinition.class))).thenReturn(infraDef.getUuid());
    when(appService.getAccountIdByAppId(anyString())).thenReturn(ACCOUNT_ID);

    infrastructureDefinitionService.save(infraDef, false, true);
    verify(infrastructureDefinitionService, times(0)).validateInfraDefinition(any(InfrastructureDefinition.class));
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void asgFieldUpdateShouldThrowException() {
    InfrastructureDefinition oldInfraDef =
        InfrastructureDefinition.builder()
            .deploymentType(DeploymentType.AMI)
            .cloudProviderType(CloudProviderType.AWS)
            .infrastructure(AwsAmiInfrastructure.builder().asgIdentifiesWorkload(true).build())
            .build();
    InfrastructureDefinition newInfraDef =
        InfrastructureDefinition.builder()
            .deploymentType(DeploymentType.AMI)
            .cloudProviderType(CloudProviderType.AWS)
            .infrastructure(AwsAmiInfrastructure.builder().asgIdentifiesWorkload(false).build())
            .build();

    assertThatThrownBy(() -> infrastructureDefinitionService.validateImmutableFields(newInfraDef, oldInfraDef))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Asg Uniquely Identifies Workload");
  }

  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Category(UnitTests.class)
  //  public void testListHostedZones() {
  //    InfrastructureDefinition awsEcsInfra = getValidInfra(AWS_ECS, true);
  //  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void googleKubernetesEngineSetDefaultsTest() {
    InfrastructureDefinition googleKubernetesInfraDef =
        InfrastructureDefinition.builder().infrastructure(GoogleKubernetesEngine.builder().build()).build();

    infrastructureDefinitionService.setDefaults(googleKubernetesInfraDef);

    assertThat(((GoogleKubernetesEngine) (googleKubernetesInfraDef.getInfrastructure())).getNamespace())
        .isEqualTo(DEFAULT);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void googleKubernetesEngineSetDefaultsNoChangeTest() {
    InfrastructureDefinition googleKubernetesInfraDef =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().namespace(USER_INPUT_NAMESPACE).build())
            .build();

    infrastructureDefinitionService.setDefaults(googleKubernetesInfraDef);

    assertThat(((GoogleKubernetesEngine) (googleKubernetesInfraDef.getInfrastructure())).getNamespace())
        .isEqualTo(USER_INPUT_NAMESPACE);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void directKubernetesSetDefaultsTest() {
    InfrastructureDefinition directKubernetesInfraDef =
        InfrastructureDefinition.builder().infrastructure(DirectKubernetesInfrastructure.builder().build()).build();

    infrastructureDefinitionService.setDefaults(directKubernetesInfraDef);

    assertThat(((DirectKubernetesInfrastructure) (directKubernetesInfraDef.getInfrastructure())).getNamespace())
        .isEqualTo(DEFAULT);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void directKubernetesSetDefaultsNoChangeTest() {
    InfrastructureDefinition directKubernetesInfraDef =
        InfrastructureDefinition.builder()
            .infrastructure(DirectKubernetesInfrastructure.builder().namespace(USER_INPUT_NAMESPACE).build())
            .build();

    infrastructureDefinitionService.setDefaults(directKubernetesInfraDef);

    assertThat(((DirectKubernetesInfrastructure) (directKubernetesInfraDef.getInfrastructure())).getNamespace())
        .isEqualTo(USER_INPUT_NAMESPACE);
  }
}