package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Maps.newHashMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Activity.Type.Command;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.FeatureName.DISABLE_ADDING_SERVICE_VARS_TO_ECS_SPEC;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.AWS_ECS_SERVICE_SETUP;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.SweepingOutputInstance;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import software.wings.WingsBaseTest;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.HostElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.EcsContainerDetails;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.ecs.request.EcsBGListenerUpdateRequest;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.helpers.ext.ecs.request.EcsServiceDeployRequest;
import software.wings.helpers.ext.ecs.request.EcsServiceSetupRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsServiceDeployResponse;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;
import java.util.Map;

public class EcsStateHelperTest extends WingsBaseTest {
  @Mock private FeatureFlagService featureFlagService;
  @Mock private SweepingOutputService sweepingOutputService;
  @Inject @InjectMocks private EcsStateHelper helper;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testBuildContainerSetupParams() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });
    EcsContainerTask containerTask = new EcsContainerTask();
    containerTask.setContainerDefinitions(
        singletonList(ContainerDefinition.builder().commands(singletonList("Command")).build()));
    containerTask.setAdvancedConfig("AdvancedConfig");
    EcsSetupStateConfig config =
        EcsSetupStateConfig.builder()
            .app(anApplication().uuid(APP_ID).name(APP_NAME).build())
            .env(anEnvironment().uuid(ENV_ID).name(ENV_NAME).build())
            .containerTask(containerTask)
            .serviceSteadyStateTimeout(10)
            .infrastructureMapping(anEcsInfrastructureMapping()
                                       .withUuid(INFRA_MAPPING_ID)
                                       .withRegion("us-east-1")
                                       .withVpcId("vpc-id")
                                       .withAssignPublicIp(true)
                                       .withLaunchType("Ec2")
                                       .build())
            .ecsServiceName("EcsService")
            .clusterName("EcsCluster")
            .blueGreen(true)
            .useRoute53DNSSwap(true)
            .serviceDiscoveryService1JSON("Json1")
            .serviceDiscoveryService2JSON("Json2")
            .parentRecordHostedZoneId("ParentZone")
            .parentRecordName("ParentRecord")
            .ecsServiceSpecification(EcsServiceSpecification.builder().serviceSpecJson("ServiceSpecJson").build())
            .awsAutoScalarConfigs(singletonList(AwsAutoScalarConfig.builder()
                                                    .scalableTargetJson("ScalbaleTarget")
                                                    .scalingPolicyForTarget("ScalablePolicy")
                                                    .build()))
            .build();
    ContainerSetupParams containerSetupParams = helper.buildContainerSetupParams(mockContext, config);
    assertThat(containerSetupParams).isNotNull();
    assertThat(containerSetupParams instanceof EcsSetupParams).isTrue();
    EcsSetupParams ecsSetupParams = (EcsSetupParams) containerSetupParams;
    assertThat(ecsSetupParams.getRegion()).isEqualTo("us-east-1");
    assertThat(ecsSetupParams.getVpcId()).isEqualTo("vpc-id");
    assertThat(ecsSetupParams.getLaunchType()).isEqualTo("Ec2");
    assertThat(ecsSetupParams.getEcsServiceSpecification().getServiceSpecJson()).isEqualTo("ServiceSpecJson");
    assertThat(ecsSetupParams.getNewAwsAutoScalarConfigList().size()).isEqualTo(1);
    assertThat(ecsSetupParams.getServiceDiscoveryService1JSON()).isEqualTo("Json1");
    assertThat(ecsSetupParams.getServiceDiscoveryService2JSON()).isEqualTo("Json2");
    assertThat(ecsSetupParams.getParentRecordHostedZoneId()).isEqualTo("ParentZone");
    assertThat(ecsSetupParams.getParentRecordName()).isEqualTo("ParentRecord");
    assertThat(ecsSetupParams.getClusterName()).isEqualTo("EcsCluster");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCreateActivity() {
    ActivityService mockActivityService = mock(ActivityService.class);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    Application application = anApplication().uuid(APP_ID).name(APP_NAME).build();
    Environment environment = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(application).when(mockContext).fetchRequiredApp();
    doReturn(environment).when(mockContext).getEnv();
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(mockParams).when(mockContext).getContextElement(any());
    EmbeddedUser user = EmbeddedUser.builder().name("user").email("user@harness.io").build();
    doReturn(user).when(mockParams).getCurrentUser();
    helper.createActivity(mockContext, "CommandName", "StateType", AWS_ECS_SERVICE_SETUP, mockActivityService);
    ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
    verify(mockActivityService).save(captor.capture());
    Activity activity = captor.getValue();
    assertThat(activity).isNotNull();
    assertThat(activity.getAppId()).isEqualTo(APP_ID);
    assertThat(activity.getType()).isEqualTo(Command);
    assertThat(activity.getCommandName()).isEqualTo("CommandName");
    assertThat(activity.getStatus()).isEqualTo(RUNNING);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testQueueDelegateTaskForEcsListenerUpdate() {
    Application application = anApplication().uuid(APP_ID).name(APP_NAME).build();
    AwsConfig awsConfig = AwsConfig.builder().build();
    DelegateService mockService = mock(DelegateService.class);
    EcsInfrastructureMapping mapping = anEcsInfrastructureMapping()
                                           .withUuid(INFRA_MAPPING_ID)
                                           .withRegion("us-east-1")
                                           .withVpcId("vpc-id")
                                           .withAssignPublicIp(true)
                                           .withLaunchType("Ec2")
                                           .build();
    EcsListenerUpdateRequestConfigData configData = EcsListenerUpdateRequestConfigData.builder()
                                                        .prodListenerArn("ProdLArn")
                                                        .stageListenerArn("StageLArn")
                                                        .region("us-east-1")
                                                        .serviceName("ServiceName")
                                                        .downsizeOldService(true)
                                                        .targetGroupForNewService("NewTgt")
                                                        .targetGroupForExistingService("OldTgt")
                                                        .build();
    helper.queueDelegateTaskForEcsListenerUpdate(
        application, awsConfig, mockService, mapping, ACTIVITY_ID, ENV_ID, "CommandName", configData, emptyList());
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(mockService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(2).isEqualTo(delegateTask.getData().getParameters().length);
    assertThat(delegateTask.getData().getParameters()[0] instanceof EcsBGListenerUpdateRequest).isTrue();
    EcsBGListenerUpdateRequest params = (EcsBGListenerUpdateRequest) delegateTask.getData().getParameters()[0];
    assertThat(params).isNotNull();
    assertThat(params.getProdListenerArn()).isEqualTo("ProdLArn");
    assertThat(params.getStageListenerArn()).isEqualTo("StageLArn");
    assertThat(params.getTargetGroupForExistingService()).isEqualTo("OldTgt");
    assertThat(params.getTargetGroupForNewService()).isEqualTo("NewTgt");
    assertThat(params.getCommandName()).isEqualTo("CommandName");
    assertThat(params.getServiceName()).isEqualTo("ServiceName");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testBuildContainerServiceElement() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });
    ContainerSetupCommandUnitExecutionData data = ContainerSetupCommandUnitExecutionData.builder()
                                                      .containerServiceName("ContainerServiceName")
                                                      .instanceCountForLatestVersion(2)
                                                      .build();
    ImageDetails details = ImageDetails.builder().name("imgName").tag("imgTag").build();
    Logger mockLogger = mock(Logger.class);
    CommandStateExecutionData executionData =
        aCommandStateExecutionData()
            .withServiceId(SERVICE_ID)
            .withClusterName(CLUSTER_NAME)
            .withContainerSetupParams(anEcsSetupParams()
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withNewAwsAutoScalarConfigList(
                                              singletonList(AwsAutoScalarConfig.builder().resourceId("ResId").build()))
                                          .build())
            .build();
    doReturn(executionData).when(mockContext).getStateExecutionData();
    ContainerServiceElement element = helper.buildContainerServiceElement(
        mockContext, data, SUCCESS, details, "3", "2", "fixedInstances", RESIZE_NEW_FIRST, 10, mockLogger);
    assertThat(element).isNotNull();
    assertThat(element.getUuid()).isEqualTo(SERVICE_ID);
    assertThat(element.getName()).isEqualTo("ContainerServiceName");
    assertThat(element.getImage()).isEqualTo("imgName:imgTag");
    assertThat(element.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(element.getNewServiceAutoScalarConfig().size()).isEqualTo(1);
    assertThat(element.getNewServiceAutoScalarConfig().get(0).getResourceId()).isEqualTo("ResId");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testPrepareBagForEcsSetUp() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });
    ArtifactCollectionUtils mockUtils = mock(ArtifactCollectionUtils.class);
    ServiceResourceService mockService = mock(ServiceResourceService.class);
    InfrastructureMappingService mockMappingService = mock(InfrastructureMappingService.class);
    SettingsService mockSettingService = mock(SettingsService.class);
    SecretManager mockSecretManaer = mock(SecretManager.class);
    PhaseElement phaseElement =
        PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build()).build();
    doReturn(phaseElement).when(mockContext).getContextElement(any(), anyString());
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(mockParams).when(mockContext).getContextElement(any());
    Application app = anApplication().uuid(APP_ID).name(APP_NAME).build();
    Environment env = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(app).when(mockParams).fetchRequiredApp();
    doReturn(env).when(mockParams).getEnv();
    Artifact artifact = anArtifact().build();
    doReturn(artifact).when(mockContext).getDefaultArtifactForService(anyString());
    ImageDetails details = ImageDetails.builder().name("imgName").tag("imgTag").build();
    doReturn(details).when(mockUtils).fetchContainerImageDetails(any(), anyString());
    Service service = Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build();
    doReturn(service).when(mockService).getWithDetails(anyString(), anyString());
    EcsContainerTask containerTask = new EcsContainerTask();
    containerTask.setUuid("ContainerTaskId");
    EcsInfrastructureMapping mapping = anEcsInfrastructureMapping()
                                           .withUuid(INFRA_MAPPING_ID)
                                           .withRegion("us-east-1")
                                           .withVpcId("vpc-id")
                                           .withAssignPublicIp(true)
                                           .withLaunchType("Ec2")
                                           .build();
    doReturn(mapping).when(mockMappingService).get(anyString(), anyString());
    SettingAttribute settingAttribute =
        aSettingAttribute().withUuid(SETTING_ID).withValue(AwsConfig.builder().build()).build();
    doReturn(settingAttribute).when(mockSettingService).get(anyString());
    doReturn(emptyList()).when(mockSecretManaer).getEncryptionDetails(any(), anyString(), anyString());
    EcsServiceSpecification specification = EcsServiceSpecification.builder().serviceId(SERVICE_ID).build();
    doReturn(specification).when(mockService).getEcsServiceSpecification(anyString(), anyString());
    EcsSetUpDataBag bag = helper.prepareBagForEcsSetUp(
        mockContext, 0, mockUtils, mockService, mockMappingService, mockSettingService, mockSecretManaer);
    assertThat(bag).isNotNull();
    assertThat(bag.getService()).isNotNull();
    assertThat(bag.getService().getUuid()).isEqualTo(SERVICE_ID);
    assertThat(bag.getEnvironment()).isNotNull();
    assertThat(bag.getEnvironment().getUuid()).isEqualTo(ENV_ID);
    assertThat(bag.getApplication()).isNotNull();
    assertThat(bag.getApplication().getUuid()).isEqualTo(APP_ID);
    assertThat(bag.getImageDetails()).isNotNull();
    assertThat(bag.getImageDetails().getName()).isEqualTo("imgName");
    assertThat(bag.getImageDetails().getTag()).isEqualTo("imgTag");
    assertThat(bag.getServiceSpecification()).isNotNull();
    assertThat(bag.getServiceSpecification().getServiceId()).isEqualTo(SERVICE_ID);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRenderEcsSetupContextVariables() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });

    doReturn(false)
        .doReturn(true)
        .when(featureFlagService)
        .isEnabled(eq(DISABLE_ADDING_SERVICE_VARS_TO_ECS_SPEC), anyString());
    Map<String, String> serviceVars = newHashMap("sk", "sv");
    Map<String, String> safeDisplayVars = newHashMap("dk", "dv");
    doReturn(serviceVars).when(mockContext).getServiceVariables();
    doReturn(safeDisplayVars).when(mockContext).getSafeDisplayServiceVariables();
    EcsSetupContextVariableHolder holder = helper.renderEcsSetupContextVariables(mockContext);
    assertThat(holder).isNotNull();
    assertThat(holder.getServiceVariables().size()).isEqualTo(1);
    assertThat(holder.getServiceVariables().get("sk")).isEqualTo("sv");
    assertThat(holder.getSafeDisplayServiceVariables().size()).isEqualTo(1);
    assertThat(holder.getSafeDisplayServiceVariables().get("dk")).isEqualTo("dv");

    // DISABLE_ADDING_SERVICE_VARS_TO_ECS_SPEC = true
    holder = helper.renderEcsSetupContextVariables(mockContext);
    assertThat(holder.getServiceVariables()).isEmpty();
    assertThat(holder.getSafeDisplayServiceVariables()).isEmpty();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testPopulateFromDelegateResponse() {
    ContainerSetupCommandUnitExecutionData data = ContainerSetupCommandUnitExecutionData.builder()
                                                      .previousEcsServiceSnapshotJson("PrevJson")
                                                      .ecsServiceArn("EcsSvcArn")
                                                      .ecsRegion("us-east-1")
                                                      .targetGroupForNewService("TgtNew")
                                                      .targetGroupForExistingService("Tgtold")
                                                      .containerServiceName("ContainerServiceName")
                                                      .instanceCountForLatestVersion(2)
                                                      .ecsServiceToBeDownsized("Svc__1")
                                                      .parentRecordName("ParentRecord")
                                                      .parentRecordHostedZoneId("ParentZoneId")
                                                      .oldServiceDiscoveryArn("OldSDSArn")
                                                      .newServiceDiscoveryArn("NewSDSArn")
                                                      .useRoute53Swap(true)
                                                      .build();
    CommandStateExecutionData executionData = aCommandStateExecutionData()
                                                  .withContainerSetupParams(anEcsSetupParams()
                                                                                .withBlueGreen(true)
                                                                                .withProdListenerArn("ProdLArn")
                                                                                .withTargetGroupArn("TgtGpArn")
                                                                                .withTargetGroupArn2("TgtGpArn2")
                                                                                .build())
                                                  .build();
    ContainerServiceElement containerServiceElement = ContainerServiceElement.builder().build();
    helper.populateFromDelegateResponse(data, executionData, containerServiceElement);
    assertThat(containerServiceElement.getNewEcsServiceName()).isEqualTo("ContainerServiceName");
    assertThat(containerServiceElement.getTargetGroupForNewService()).isEqualTo("TgtNew");
    assertThat(containerServiceElement.getTargetGroupForExistingService()).isEqualTo("Tgtold");
    assertThat(containerServiceElement.getEcsRegion()).isEqualTo("us-east-1");
    assertThat(containerServiceElement.getEcsBGSetupData()).isNotNull();
    assertThat(containerServiceElement.getEcsBGSetupData().getProdEcsListener()).isEqualTo("ProdLArn");
    assertThat(containerServiceElement.getEcsBGSetupData().getEcsBGTargetGroup1()).isEqualTo("TgtGpArn");
    assertThat(containerServiceElement.getEcsBGSetupData().getEcsBGTargetGroup2()).isEqualTo("TgtGpArn2");
    assertThat(containerServiceElement.getEcsBGSetupData().getDownsizedServiceName()).isEqualTo("Svc__1");
    assertThat(containerServiceElement.getEcsBGSetupData().getParentRecordName()).isEqualTo("ParentRecord");
    assertThat(containerServiceElement.getEcsBGSetupData().getParentRecordHostedZoneId()).isEqualTo("ParentZoneId");
    assertThat(containerServiceElement.getEcsBGSetupData().getOldServiceDiscoveryArn()).isEqualTo("OldSDSArn");
    assertThat(containerServiceElement.getEcsBGSetupData().getNewServiceDiscoveryArn()).isEqualTo("NewSDSArn");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetStateExecutionData() {
    EcsSetUpDataBag bag = EcsSetUpDataBag.builder()
                              .service(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build())
                              .application(anApplication().uuid(APP_ID).name(APP_NAME).build())
                              .ecsInfrastructureMapping(anEcsInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withClusterName(CLUSTER_NAME)
                                                            .withRegion("us-east-1")
                                                            .withVpcId("vpc-id")
                                                            .withAssignPublicIp(true)
                                                            .withLaunchType("Ec2")
                                                            .build())
                              .build();
    CommandStateExecutionData data =
        helper.getStateExecutionData(bag, "CommandName", null, Activity.builder().uuid(ACTIVITY_ID).build());
    assertThat(data).isNotNull();
    assertThat(data.getAppId()).isEqualTo(APP_ID);
    assertThat(data.getCommandName()).isEqualTo("CommandName");
    assertThat(data.getServiceName()).isEqualTo(SERVICE_NAME);
    assertThat(data.getServiceId()).isEqualTo(SERVICE_ID);
    assertThat(data.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(data.getClusterName()).isEqualTo(CLUSTER_NAME);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCreateAndQueueDelegateTaskForEcsServiceSetUp() {
    EcsSetUpDataBag bag = EcsSetUpDataBag.builder()
                              .service(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build())
                              .application(anApplication().uuid(APP_ID).name(APP_NAME).build())
                              .environment(anEnvironment().uuid(ENV_ID).name(ENV_NAME).build())
                              .ecsInfrastructureMapping(anEcsInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withClusterName(CLUSTER_NAME)
                                                            .withRegion("us-east-1")
                                                            .withVpcId("vpc-id")
                                                            .withAssignPublicIp(true)
                                                            .withLaunchType("Ec2")
                                                            .build())
                              .awsConfig(AwsConfig.builder().build())
                              .encryptedDataDetails(emptyList())
                              .build();
    DelegateService mockDelegateService = mock(DelegateService.class);
    helper.createAndQueueDelegateTaskForEcsServiceSetUp(EcsServiceSetupRequest.builder().build(), bag,
        Activity.builder().uuid(ACTIVITY_ID).build(), mockDelegateService);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(mockDelegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(2).isEqualTo(delegateTask.getData().getParameters().length);
    assertThat(delegateTask.getData().getParameters()[0] instanceof EcsServiceSetupRequest).isTrue();
    assertThat(delegateTask.getAppId()).isEqualTo(APP_ID);
    assertThat(delegateTask.getWaitId()).isEqualTo(ACTIVITY_ID);
    assertThat(delegateTask.getEnvId()).isEqualTo(ENV_ID);
    assertThat(delegateTask.getInfrastructureMappingId()).isEqualTo(INFRA_MAPPING_ID);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleDelegateResponseForEcsDeploy() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    CommandStateExecutionData executionData = aCommandStateExecutionData().build();
    doReturn(executionData).when(mockContext).getStateExecutionData();
    EcsCommandExecutionResponse delegateResponse =
        EcsCommandExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .ecsCommandResponse(
                EcsServiceDeployResponse.builder()
                    .containerInfos(singletonList(ContainerInfo.builder().containerId("ContId").ip("ip").build()))
                    .build())
            .build();
    ContainerDeploymentManagerHelper mockHelper = mock(ContainerDeploymentManagerHelper.class);
    doReturn(singletonList(anInstanceStatusSummary()
                               .withInstanceElement(anInstanceElement()
                                                        .dockerId("DockerId")
                                                        .hostName("HostName")
                                                        .host(HostElement.builder().build())
                                                        .ecsContainerDetails(EcsContainerDetails.builder().build())
                                                        .build())
                               .build()))
        .doReturn(emptyList())
        .when(mockHelper)
        .getInstanceStatusSummaries(any(), anyList());
    ActivityService mockService = mock(ActivityService.class);
    doReturn(null).when(sweepingOutputService).save(any());
    doReturn("").when(mockContext).appendStateExecutionId(anyString());
    doReturn(SweepingOutputInstance.builder()).when(mockContext).prepareSweepingOutputBuilder(any());
    ExecutionResponse response = helper.handleDelegateResponseForEcsDeploy(
        mockContext, ImmutableMap.of(ACTIVITY_ID, delegateResponse), false, mockService, null, mockHelper);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
    List<ContextElement> contextElements = response.getContextElements();
    assertThat(contextElements).isNotNull();
    assertThat(contextElements.size()).isEqualTo(1);
    ContextElement contextElement = contextElements.get(0);
    assertThat(contextElement instanceof InstanceElementListParam).isTrue();
    InstanceElementListParam listParam = (InstanceElementListParam) contextElement;
    assertThat(listParam.getInstanceElements()).isNotNull();
    assertThat(listParam.getInstanceElements().size()).isEqualTo(1);
    assertThat(listParam.getInstanceElements().get(0).getHostName()).isEqualTo("HostName");
    assertThat(listParam.getInstanceElements().get(0).getDockerId()).isEqualTo("DockerId");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCreateAndQueueDelegateTaskForEcsServiceDeploy() {
    EcsDeployDataBag bag =
        EcsDeployDataBag.builder()
            .service(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build())
            .app(anApplication().uuid(APP_ID).name(APP_NAME).build())
            .env(anEnvironment().uuid(ENV_ID).name(ENV_NAME).build())
            .ecsInfrastructureMapping(anEcsInfrastructureMapping()
                                          .withUuid(INFRA_MAPPING_ID)
                                          .withClusterName(CLUSTER_NAME)
                                          .withRegion("us-east-1")
                                          .withVpcId("vpc-id")
                                          .withAssignPublicIp(true)
                                          .withLaunchType("Ec2")
                                          .build())
            .awsConfig(AwsConfig.builder().build())
            .encryptedDataDetails(emptyList())
            .containerElement(ContainerServiceElement.builder().serviceSteadyStateTimeout(10).build())
            .build();
    DelegateService mockDelegateService = mock(DelegateService.class);
    helper.createAndQueueDelegateTaskForEcsServiceDeploy(bag, EcsServiceDeployRequest.builder().build(),
        Activity.builder().uuid(ACTIVITY_ID).build(), mockDelegateService);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(mockDelegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(2).isEqualTo(delegateTask.getData().getParameters().length);
    assertThat(delegateTask.getData().getParameters()[0] instanceof EcsServiceDeployRequest).isTrue();
    assertThat(delegateTask.getAppId()).isEqualTo(APP_ID);
    assertThat(delegateTask.getWaitId()).isEqualTo(ACTIVITY_ID);
    assertThat(delegateTask.getEnvId()).isEqualTo(ENV_ID);
    assertThat(delegateTask.getInfrastructureMappingId()).isEqualTo(INFRA_MAPPING_ID);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetTimeout() {
    long timeout =
        helper.getTimeout(EcsDeployDataBag.builder()
                              .containerElement(ContainerServiceElement.builder().serviceSteadyStateTimeout(10).build())
                              .build());

    assertThat(timeout).isEqualTo(40l);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testPrepareBagForEcsDeploy() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });
    ServiceResourceService mockService = mock(ServiceResourceService.class);
    InfrastructureMappingService mockMappingService = mock(InfrastructureMappingService.class);
    SettingsService mockSettingService = mock(SettingsService.class);
    SecretManager mockSecretManaer = mock(SecretManager.class);
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(mockParams).when(mockContext).getContextElement(any());
    Application app = anApplication().uuid(APP_ID).name(APP_NAME).build();
    Environment env = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(app).when(mockParams).fetchRequiredApp();
    doReturn(env).when(mockParams).getEnv();
    PhaseElement phaseElement =
        PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build()).build();
    ContainerRollbackRequestElement requestElement = ContainerRollbackRequestElement.builder().build();
    doReturn(phaseElement).doReturn(requestElement).when(mockContext).getContextElement(any(), anyString());
    Service service = Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build();
    doReturn(service).when(mockService).getWithDetails(anyString(), anyString());
    EcsInfrastructureMapping mapping = anEcsInfrastructureMapping()
                                           .withUuid(INFRA_MAPPING_ID)
                                           .withRegion("us-east-1")
                                           .withVpcId("vpc-id")
                                           .withAssignPublicIp(true)
                                           .withLaunchType("Ec2")
                                           .build();
    doReturn(mapping).when(mockMappingService).get(anyString(), anyString());
    SettingAttribute settingAttribute =
        aSettingAttribute().withUuid(SETTING_ID).withValue(AwsConfig.builder().build()).build();
    doReturn(settingAttribute).when(mockSettingService).get(anyString());
    doReturn(emptyList()).when(mockSecretManaer).getEncryptionDetails(any(), anyString(), anyString());
    EcsDeployDataBag bag = helper.prepareBagForEcsDeploy(
        mockContext, mockService, mockMappingService, mockSettingService, mockSecretManaer);
    assertThat(bag).isNotNull();
    assertThat(bag.getService()).isNotNull();
    assertThat(bag.getService().getUuid()).isEqualTo(SERVICE_ID);
    assertThat(bag.getEnv()).isNotNull();
    assertThat(bag.getEnv().getUuid()).isEqualTo(ENV_ID);
    assertThat(bag.getApp()).isNotNull();
    assertThat(bag.getApp().getUuid()).isEqualTo(APP_ID);
  }
}