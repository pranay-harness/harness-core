package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.beans.OrchestrationWorkflowType.CANARY;
import static io.harness.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static io.harness.beans.OrchestrationWorkflowType.ROLLING;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandType.ENABLE;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.AwsAmiSetupExecutionData;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.UserDataSpecification;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;
import java.util.Map;

public class AwsAmiServiceSetupTest extends WingsBaseTest {
  @Mock private SettingsService mockSettingsService;
  @Mock private ServiceResourceService mockServiceResourceService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;
  @Mock private SecretManager mockSecretManager;
  @Mock private ActivityService mockActivityService;
  @Mock private LogService mockLogService;
  @Mock private DelegateService mockDelegateService;

  @InjectMocks private AwsAmiServiceSetup state = new AwsAmiServiceSetup("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    String asgName = "foo";
    state.setAutoScalingGroupName(asgName);
    state.setMaxInstances(2);
    state.setDesiredInstances(1);
    state.setMinInstances(0);
    state.setAutoScalingSteadyStateTimeout(10);
    state.setBlueGreen(false);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(asgName).when(mockContext).renderExpression(anyString());
    PhaseElement phaseElement =
        PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build()).build();
    doReturn(phaseElement).when(mockContext).getContextElement(any(), anyString());
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(EmbeddedUser.builder().email("user@harness.io").name("user").build()).when(mockParams).getCurrentUser();
    doReturn(mockParams).when(mockContext).getContextElement(any());
    String revision = "ami-1234";
    Artifact artifact = anArtifact().withRevision(revision).build();
    doReturn(artifact).when(mockContext).getDefaultArtifactForService(anyString());
    Application application = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();
    doReturn(application).when(mockParams).getApp();
    Environment environment = anEnvironment().uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(environment).when(mockParams).getEnv();
    Service service = Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build();
    doReturn(service).when(mockServiceResourceService).getWithDetails(anyString(), anyString());
    String classicLb = "classicLb";
    String targetGroup = "targetGp";
    String baseAsg = "baseAsg";
    AwsAmiInfrastructureMapping infrastructureMapping = anAwsAmiInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withRegion("us-east-1")
                                                            .withClassicLoadBalancers(singletonList(classicLb))
                                                            .withTargetGroupArns(singletonList(targetGroup))
                                                            .withAutoScalingGroupName(baseAsg)
                                                            .build();
    doReturn(infrastructureMapping).when(mockInfrastructureMappingService).get(anyString(), anyString());
    SettingAttribute cloudProvider = aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    doReturn(cloudProvider).when(mockSettingsService).get(anyString());
    doReturn(emptyList()).when(mockSecretManager).getEncryptionDetails(any(), anyString(), anyString());
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
    doReturn(true).when(mockLogService).batchedSaveCommandUnitLogs(anyString(), anyString(), any());
    String userData = "userData";
    UserDataSpecification userDataSpecification = UserDataSpecification.builder().data(userData).build();
    doReturn(userDataSpecification).when(mockServiceResourceService).getUserDataSpecification(anyString(), anyString());
    ExecutionResponse response = state.execute(mockContext);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(mockDelegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(1).isEqualTo(delegateTask.getData().getParameters().length);
    assertThat(delegateTask.getData().getParameters()[0] instanceof AwsAmiServiceSetupRequest).isTrue();
    AwsAmiServiceSetupRequest params = (AwsAmiServiceSetupRequest) delegateTask.getData().getParameters()[0];
    assertThat(params.getInfraMappingClassisLbs().size()).isEqualTo(1);
    assertThat(params.getInfraMappingClassisLbs().get(0)).isEqualTo(classicLb);
    assertThat(params.getInfraMappingTargetGroupArns().size()).isEqualTo(1);
    assertThat(params.getInfraMappingTargetGroupArns().get(0)).isEqualTo(targetGroup);
    assertThat(params.getNewAsgNamePrefix()).isEqualTo(asgName);
    assertThat(params.getMinInstances()).isEqualTo(0);
    assertThat(params.getMaxInstances()).isEqualTo(2);
    assertThat(params.getDesiredInstances()).isEqualTo(1);
    assertThat(params.getArtifactRevision()).isEqualTo(revision);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    AwsAmiSetupExecutionData stateData = AwsAmiSetupExecutionData.builder().build();
    doReturn(stateData).when(mockContext).getStateExecutionData();
    String newAsgName = "foo__2";
    AwsAmiServiceSetupResponse delegateResponse = AwsAmiServiceSetupResponse.builder()
                                                      .executionStatus(SUCCESS)
                                                      .newAsgName(newAsgName)
                                                      .minInstances(0)
                                                      .maxInstances(2)
                                                      .desiredInstances(1)
                                                      .build();
    ExecutionResponse response = state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateResponse));
    assertThat(response).isNotNull();
    assertThat(response.getNotifyElements()).isNotNull();
    assertThat(response.getNotifyElements().size()).isEqualTo(1);
    ContextElement contextElement = response.getNotifyElements().get(0);
    assertThat(contextElement instanceof AmiServiceSetupElement).isTrue();
    AmiServiceSetupElement amiServiceSetupElement = (AmiServiceSetupElement) contextElement;
    assertThat(amiServiceSetupElement.getNewAutoScalingGroupName()).isEqualTo(newAsgName);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testValidateFields() {
    AwsAmiServiceSetup localState = new AwsAmiServiceSetup("stateName2");
    localState.setUseCurrentRunningCount(false);
    localState.setMinInstances(2);
    localState.setDesiredInstances(1);
    localState.setMaxInstances(0);
    Map<String, String> fieldMap = localState.validateFields();
    assertThat(fieldMap.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testIsBlueGreenWorkflow() {
    ExecutionContext execContext = mock(ExecutionContext.class);
    doReturn(BASIC)
        .doReturn(CANARY)
        .doReturn(ROLLING)
        .doReturn(MULTI_SERVICE)
        .doReturn(BLUE_GREEN)
        .when(execContext)
        .getOrchestrationWorkflowType();

    assertThat(state.isBlueGreenWorkflow(execContext)).isFalse();
    assertThat(state.isBlueGreenWorkflow(execContext)).isFalse();
    assertThat(state.isBlueGreenWorkflow(execContext)).isFalse();
    assertThat(state.isBlueGreenWorkflow(execContext)).isFalse();
    assertThat(state.isBlueGreenWorkflow(execContext)).isTrue();
  }
}