package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.beans.OrchestrationWorkflowType.CANARY;
import static io.harness.rule.OwnerRule.ANIL;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.SweepingOutputInstanceBuilder;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.rule.Owner;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.api.AmiServiceTrafficShiftAlbSetupElement;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.beans.Activity;
import software.wings.beans.AwsConfig;
import software.wings.beans.Service;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.service.impl.aws.model.AwsAmiServiceTrafficShiftAlbDeployRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionData;

import java.util.Collections;

public class AwsAmiServiceTrafficShiftAlbDeployStateTest extends WingsBaseTest {
  @Mock private ServiceResourceService serviceResourceService;
  @Mock ArtifactStreamService artifactStreamService;
  @Mock ActivityService activityService;
  @Mock DelegateService delegateService;
  @Mock LogService logService;
  @Mock SweepingOutputService sweepingOutputService;
  @Mock AwsStateHelper awsStateHelper;
  @Mock AwsAmiServiceStateHelper awsAmiServiceHelper;

  @InjectMocks
  private final AwsAmiServiceTrafficShiftAlbDeployState state =
      new AwsAmiServiceTrafficShiftAlbDeployState("stateName");

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testExecuteSuccess() {
    ExecutionContextImpl mockContext = initializeMockSetup(true);
    ExecutionResponse response = state.execute(mockContext);
    verifyDelegateTaskCreationResult(response);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testExecuteFailure() {
    ExecutionContextImpl mockContext = initializeMockSetup(false);
    ExecutionResponse response = state.execute(mockContext);
    assertThat(response.getExecutionStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = initializeMockSetup(true);
    AwsAmiServiceDeployResponse amiServiceDeployResponse =
        AwsAmiServiceDeployResponse.builder()
            .delegateMetaInfo(DelegateMetaInfo.builder().build())
            .instancesAdded(Collections.singletonList(new Instance()))
            .build();
    ExecutionResponse response =
        state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, amiServiceDeployResponse));
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testHandleAsyncFailureResponse() {
    ExecutionContextImpl mockContext = initializeMockSetup(true);
    AwsAmiServiceDeployResponse amiServiceDeployResponse =
        AwsAmiServiceDeployResponse.builder()
            .delegateMetaInfo(DelegateMetaInfo.builder().build())
            .instancesAdded(Collections.singletonList(new Instance()))
            .build();
    doThrow(Exception.class).when(state.awsAmiServiceHelper).populateAlbTrafficShiftSetupData(any());
    ExecutionResponse response =
        state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, amiServiceDeployResponse));
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(FAILED);
  }

  @NotNull
  private ExecutionContextImpl initializeMockSetup(boolean isSuccess) {
    state.setInstanceUnitType(PERCENTAGE);
    state.setInstanceCountExpr("100");

    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    when(mockContext.renderExpression(anyString())).thenAnswer((Answer<String>) invocation -> {
      Object[] args = invocation.getArguments();
      return (String) args[0];
    });
    doReturn(CANARY).doReturn(BLUE_GREEN).when(mockContext).getOrchestrationWorkflowType();

    //    ServiceResourceService serviceResourceService = mock((ServiceResourceService.class));
    //    ArtifactStreamService artifactStreamService = mock(ArtifactStreamService.class);
    //    ActivityService activityService = mock(ActivityService.class);
    //    DelegateService delegateService = mock(DelegateService.class);
    //    LogService logService = mock(LogService.class);
    //    SweepingOutputService sweepingOutputService = mock(SweepingOutputService.class);
    //    AwsStateHelper awsStateHelper = mock(AwsStateHelper.class);
    //    AwsAmiServiceStateHelper awsAmiServiceHelper = mock(AwsAmiServiceStateHelper.class);

    on(state).set("serviceResourceService", serviceResourceService);
    on(state).set("artifactStreamService", artifactStreamService);
    on(state).set("activityService", activityService);
    on(state).set("delegateService", delegateService);
    on(state).set("logService", logService);
    on(state).set("sweepingOutputService", sweepingOutputService);
    on(state).set("awsStateHelper", awsStateHelper);
    on(state).set("awsAmiServiceHelper", awsAmiServiceHelper);

    LbDetailsForAlbTrafficShift lbDetails = LbDetailsForAlbTrafficShift.builder()
                                                .loadBalancerName("lbName")
                                                .loadBalancerArn("lbArn")
                                                .listenerPort("port")
                                                .listenerArn("listArn")
                                                .useSpecificRule(true)
                                                .ruleArn("ruleArn")
                                                .prodTargetGroupName("prodTarget")
                                                .prodTargetGroupArn("prodTargetArn")
                                                .stageTargetGroupName("stageTarget")
                                                .stageTargetGroupArn("stageTargetArn")
                                                .build();

    AmiServiceTrafficShiftAlbSetupElement setupElement =
        AmiServiceTrafficShiftAlbSetupElement.builder()
            .newAutoScalingGroupName("newAsg")
            .oldAutoScalingGroupName("oldAsg")
            .baseScalingPolicyJSONs(Collections.singletonList("policyJson"))
            .minInstances(1)
            .maxInstances(4)
            .desiredInstances(2)
            .autoScalingSteadyStateTimeout(10)
            .commandName("COMMAND_NAME")
            .oldAsgNames(Collections.emptyList())
            .preDeploymentData(AwsAmiPreDeploymentData.builder().build())
            .detailsWithTargetGroups(Collections.singletonList(lbDetails))
            .build();
    doReturn(setupElement).when(mockContext).getContextElement(any());

    AwsAmiTrafficShiftAlbData trafficShiftAlbData =
        AwsAmiTrafficShiftAlbData.builder()
            .artifact(anArtifact().withUuid(ARTIFACT_ID).build())
            .app(anApplication().uuid(APP_ID).build())
            .service(Service.builder().build())
            .env(anEnvironment().uuid(ENV_ID).build())
            .awsConfig(AwsConfig.builder().build())
            .infrastructureMapping(
                anAwsAmiInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withSpotinstElastiGroupJson("json").build())
            .awsEncryptedDataDetails(emptyList())
            .region("region")
            .serviceId("serviceId")
            .currentUser(EmbeddedUser.builder().build())
            .build();
    doReturn(trafficShiftAlbData).when(awsAmiServiceHelper).populateAlbTrafficShiftSetupData(mockContext);

    doReturn(AmiArtifactStream.builder().build()).when(artifactStreamService).get(anyString());
    doReturn(Activity.builder().uuid(ACTIVITY_ID).commandUnits(singletonList(new AmiCommandUnit())).build())
        .when(activityService)
        .save(any());
    doReturn(ServiceCommand.Builder.aServiceCommand()
                 .withName("ASG_COMMAND_NAME")
                 .withCommand(Command.Builder.aCommand().build())
                 .build())
        .when(serviceResourceService)
        .getCommandByName(any(), any(), any(), any());
    doReturn(emptyList()).when(serviceResourceService).getFlattenCommandUnitList(any(), any(), any(), any());

    SweepingOutputInstanceBuilder instanceBuilder = SweepingOutputInstance.builder();
    doReturn(instanceBuilder).when(mockContext).prepareSweepingOutputBuilder(any());
    doReturn("ExecutionId").when(mockContext).appendStateExecutionId(anyString());
    doReturn(SweepingOutputInstance.builder().build()).when(sweepingOutputService).save(any());

    InstanceElement instanceElement = new InstanceElement();
    instanceElement.setNewInstance(true);
    instanceElement.setUuid("id");
    instanceElement.setHostName("ec2-instance");
    doReturn(singletonList(instanceElement)).when(awsStateHelper).generateInstanceElements(any(), any(), any());

    if (!isSuccess) {
      doThrow(Exception.class).when(delegateService).queueTask(any());
    }

    AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData = AwsAmiDeployStateExecutionData.builder().build();
    doReturn(awsAmiDeployStateExecutionData).when(mockContext).getStateExecutionData();
    doReturn(Activity.builder().uuid(ACTIVITY_ID).commandUnits(singletonList(new AmiCommandUnit())).build())
        .when(state.activityService)
        .get(anyString(), anyString());
    return mockContext;
  }

  private void verifyDelegateTaskCreationResult(ExecutionResponse response) {
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(state.delegateService, times(1)).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getData().getParameters()).isNotNull();
    assertThat(delegateTask.getData().getParameters()[0] instanceof AwsAmiServiceTrafficShiftAlbDeployRequest).isTrue();

    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
    StateExecutionData stateExecutionData = response.getStateExecutionData();
    assertThat(stateExecutionData instanceof AwsAmiDeployStateExecutionData).isTrue();
    AwsAmiDeployStateExecutionData executionData = (AwsAmiDeployStateExecutionData) stateExecutionData;
    assertThat(executionData.getNewAutoScalingGroupName()).isEqualTo("newAsg");
    assertThat(executionData.getOldAutoScalingGroupName()).isEqualTo("oldAsg");
    assertThat(executionData.getMaxInstances()).isEqualTo(4);
    assertThat(executionData.getNewInstanceData().get(0).getDesiredCount()).isEqualTo(2);
  }
}
