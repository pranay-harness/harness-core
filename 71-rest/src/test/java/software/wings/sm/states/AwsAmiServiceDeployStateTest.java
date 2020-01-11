package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static jersey.repackaged.com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandType.ENABLE;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.ContainerServiceData;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;
import java.util.Map;

public class AwsAmiServiceDeployStateTest extends WingsBaseTest {
  @Mock private AwsHelperService mockAwsHelperService;
  @Mock private SettingsService mockSettingsService;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private ServiceTemplateService mockServiceTemplateService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;
  @Mock private ArtifactStreamService mockArtifactStreamService;
  @Mock private SecretManager mockSecretManager;
  @Mock private EncryptionService mockEncryptionService;
  @Mock private ActivityService mockActivityService;
  @Mock private DelegateService mockDelegateService;
  @Mock private LogService mockLogService;
  @Mock private SweepingOutputService mockSweepingOutputService;
  @Mock private HostService mockHostService;
  @Mock private AwsUtils mockAwsUtils;
  @Mock private AwsAsgHelperServiceManager mockAwsAsgHelperServiceManager;
  @Mock private ServiceTemplateHelper mockServiceTemplateHelper;

  @InjectMocks private AwsAmiServiceDeployState state = new AwsAmiServiceDeployState("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    state.setInstanceUnitType(PERCENTAGE);
    state.setInstanceCount("100");
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.renderExpression(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (String) args[0];
      }
    });
    PhaseElement phaseElement =
        PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build()).build();
    String asg1 = "foo__1";
    String asg2 = "foo__2";
    String asg3 = "foo__3";
    AmiServiceSetupElement serviceSetupElement =
        AmiServiceSetupElement.builder()
            .blueGreen(false)
            .resizeStrategy(RESIZE_NEW_FIRST)
            .oldAsgNames(Lists.newArrayList(asg1, asg2))
            .preDeploymentData(
                AwsAmiPreDeploymentData.builder().asgNameToDesiredCapacity(ImmutableMap.of(asg1, 1, asg2, 1)).build())
            .autoScalingSteadyStateTimeout(10)
            .newAutoScalingGroupName(asg3)
            .minInstances(0)
            .desiredInstances(2)
            .maxInstances(2)
            .build();
    doReturn(phaseElement).when(mockContext).getContextElement(any(), anyString());
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(EmbeddedUser.builder().email("user@harness.io").name("user").build()).when(mockParams).getCurrentUser();
    doReturn(mockParams).doReturn(serviceSetupElement).when(mockContext).getContextElement(any());
    Environment environment = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(environment).when(mockParams).getEnv();
    Application application = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();
    doReturn(application).when(mockParams).getApp();
    Service service = Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build();
    doReturn(service).when(mockServiceResourceService).getWithDetails(anyString(), anyString());
    String revision = "ami-1234";
    Artifact artifact = anArtifact().withRevision(revision).build();
    doReturn(artifact).when(mockContext).getDefaultArtifactForService(anyString());
    ArtifactStream artifactStream =
        AmiArtifactStream.builder().uuid(ARTIFACT_STREAM_ID).sourceName(ARTIFACT_SOURCE_NAME).build();
    doReturn(artifactStream).when(mockArtifactStreamService).get(anyString());
    Command command = aCommand().withName("Ami-Command").withCommandType(ENABLE).build();
    ServiceCommand serviceCommand = aServiceCommand().withCommand(command).build();
    doReturn(serviceCommand)
        .when(mockServiceResourceService)
        .getCommandByName(anyString(), anyString(), anyString(), anyString());
    AmiCommandUnit commandUnit = new AmiCommandUnit();
    commandUnit.setName("Ami-Command-Unit");
    List<CommandUnit> commandUnits = singletonList(commandUnit);
    doReturn(commandUnits)
        .when(mockServiceResourceService)
        .getFlattenCommandUnitList(anyString(), anyString(), anyString(), anyString());
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).appId(APP_ID).build();
    doReturn(activity).when(mockActivityService).save(any());
    String classicLb = "classicLb";
    String targetGroup = "targetGp";
    String baseAsg = "baseAsg";
    AwsAmiInfrastructureMapping infrastructureMapping = anAwsAmiInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withEnvId(ENV_ID)
                                                            .withRegion("us-east-1")
                                                            .withClassicLoadBalancers(singletonList(classicLb))
                                                            .withTargetGroupArns(singletonList(targetGroup))
                                                            .withAutoScalingGroupName(baseAsg)
                                                            .build();
    doReturn(infrastructureMapping).when(mockInfrastructureMappingService).get(anyString(), anyString());
    SettingAttribute cloudProvider = aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    doReturn(cloudProvider).when(mockSettingsService).get(anyString());
    doReturn(emptyList()).when(mockSecretManager).getEncryptionDetails(any(), anyString(), anyString());
    doReturn(ImmutableMap.of(asg1, 1, asg2, 1, asg3, 0))
        .when(mockAwsAsgHelperServiceManager)
        .getDesiredCapacitiesOfAsgs(any(), anyList(), anyString(), anyList(), anyString());
    ExecutionResponse response = state.execute(mockContext);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(mockDelegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(1).isEqualTo(delegateTask.getData().getParameters().length);
    assertThat(delegateTask.getData().getParameters()[0] instanceof AwsAmiServiceDeployRequest).isTrue();
    AwsAmiServiceDeployRequest params = (AwsAmiServiceDeployRequest) delegateTask.getData().getParameters()[0];
    assertThat(params).isNotNull();
    assertThat(params.getNewAutoScalingGroupName()).isEqualTo(asg3);
    assertThat(params.getNewAsgFinalDesiredCount()).isEqualTo(2);
    List<AwsAmiResizeData> asgDesiredCounts = params.getAsgDesiredCounts();
    assertThat(asgDesiredCounts).isNotNull();
    assertThat(asgDesiredCounts.size()).isEqualTo(2);
    assertThat(asgDesiredCounts.get(0).getAsgName()).isEqualTo(asg1);
    assertThat(asgDesiredCounts.get(0).getDesiredCount()).isEqualTo(0);
    assertThat(asgDesiredCounts.get(1).getAsgName()).isEqualTo(asg2);
    assertThat(asgDesiredCounts.get(1).getDesiredCount()).isEqualTo(0);
    assertThat(params.getInfraMappingClassisLbs().size()).isEqualTo(1);
    assertThat(params.getInfraMappingClassisLbs().get(0)).isEqualTo(classicLb);
    assertThat(params.getInfraMappingTargetGroupArns().size()).isEqualTo(1);
    assertThat(params.getInfraMappingTargetGroupArns().get(0)).isEqualTo(targetGroup);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    AwsAmiDeployStateExecutionData stateData =
        AwsAmiDeployStateExecutionData.builder()
            .newInstanceData(
                singletonList(ContainerServiceData.builder().name("foo__1").desiredCount(1).previousCount(0).build()))
            .build();
    doReturn(stateData).when(mockContext).getStateExecutionData();
    doReturn(APP_ID).when(mockContext).getAppId();
    AwsAmiServiceDeployResponse delegateResponse =
        AwsAmiServiceDeployResponse.builder()
            .executionStatus(SUCCESS)
            .instancesAdded(singletonList(new Instance()
                                              .withInstanceId("i-1234")
                                              .withPublicDnsName("public.dns")
                                              .withPrivateDnsName("private.dns")))
            .build();
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).appId(APP_ID).build();
    doReturn(activity).when(mockActivityService).get(anyString(), anyString());
    AmiServiceSetupElement serviceSetupElement = AmiServiceSetupElement.builder().build();
    PhaseElement phaseElement =
        PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build()).build();
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(serviceSetupElement).doReturn(mockParams).when(mockContext).getContextElement(any());
    doReturn(phaseElement).when(mockContext).getContextElement(any(), anyString());
    Application application = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();
    doReturn(application).when(mockParams).getApp();
    doReturn(true).when(mockLogService).batchedSaveCommandUnitLogs(anyString(), anyString(), any());
    AwsAmiInfrastructureMapping infrastructureMapping =
        anAwsAmiInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withEnvId(ENV_ID).withRegion("us-east-1").build();
    doReturn(infrastructureMapping).when(mockInfrastructureMappingService).get(anyString(), anyString());
    Environment environment = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(environment).when(mockParams).getEnv();
    Service service = Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build();
    doReturn(service).when(mockServiceResourceService).getWithDetails(anyString(), anyString());
    Key<ServiceTemplate> serviceTemplateKey = new Key<>(ServiceTemplate.class, "collection", "id");
    doReturn(singletonList(serviceTemplateKey))
        .when(mockServiceTemplateService)
        .getTemplateRefKeysByService(anyString(), anyString(), anyString());
    String revision = "ami-1234";
    Artifact artifact = anArtifact().withRevision(revision).build();
    doReturn(artifact).when(mockContext).getDefaultArtifactForService(anyString());
    doReturn(SERVICE_TEMPLATE_ID).when(mockServiceTemplateHelper).fetchServiceTemplateId(any());
    Map<String, Object> contextMap = newHashMap();
    doReturn(contextMap).when(mockContext).asMap();
    doReturn("hostName").when(mockAwsHelperService).getHostnameFromConvention(anyMap(), anyString());
    Host host = aHost().withUuid(HOST_ID).build();
    doReturn(host).when(mockHostService).saveHost(any());
    ExecutionResponse response = state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateResponse));
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
    List<ContextElement> contextElements = response.getContextElements();
    assertThat(contextElements).isNotNull();
    assertThat(contextElements.size()).isEqualTo(1);
    ContextElement contextElement = contextElements.get(0);
    assertThat(contextElement instanceof InstanceElementListParam).isTrue();
    InstanceElementListParam listParam = (InstanceElementListParam) contextElement;
    List<InstanceElement> instanceElements = listParam.getInstanceElements();
    assertThat(instanceElements).isNotNull();
    assertThat(instanceElements.size()).isEqualTo(1);
    assertThat(instanceElements.get(0).getUuid()).isEqualTo("i-1234");
    assertThat(instanceElements.get(0).getDisplayName()).isEqualTo("public.dns");
    assertThat(instanceElements.get(0).getHostName()).isEqualTo("hostName");
  }
}