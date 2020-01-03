package software.wings.sm.states.spotinst;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static software.wings.beans.AmiDeploymentType.SPOTINST;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.rule.Owner;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.impl.spotinst.SpotInstCommandRequest.SpotInstCommandRequestBuilder;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionData;
import software.wings.sm.WorkflowStandardParams;

public class SpotInstListenerUpdateStateTest extends WingsBaseTest {
  @Mock private AppService mockAppService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;
  @Mock private DelegateService mockDelegateService;
  @Mock private SettingsService mockSettingsService;
  @Mock private ActivityService mockActivityService;
  @Mock private SpotInstStateHelper mockSpotinstStateHelper;

  @InjectMocks SpotInstListenerUpdateState state = new SpotInstListenerUpdateState("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    state.setDownsizeOldElastiGroup(true);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(INFRA_MAPPING_ID).when(mockContext).fetchInfraMappingId();
    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    doReturn(mockParams).when(mockContext).getContextElement(any());
    Environment env = anEnvironment().uuid(ENV_ID).build();
    doReturn(env).when(mockParams).fetchRequiredEnv();
    AwsAmiInfrastructureMapping infrastructureMapping =
        anAwsAmiInfrastructureMapping().withAmiDeploymentType(SPOTINST).withRegion("us-east-1").build();
    doReturn(infrastructureMapping).when(mockInfrastructureMappingService).get(anyString(), anyString());
    Application application = anApplication().appId(APP_ID).accountId(ACCOUNT_ID).uuid(APP_ID).build();
    doReturn(application).when(mockAppService).get(anyString());
    String newId = "newId";
    String oldId = "oldId";
    SpotInstSetupContextElement element =
        SpotInstSetupContextElement.builder()
            .isBlueGreen(true)
            .resizeStrategy(RESIZE_NEW_FIRST)
            .infraMappingId(INFRA_MAPPING_ID)
            .serviceId(SERVICE_ID)
            .appId(APP_ID)
            .envId(ENV_ID)
            .oldElastiGroupOriginalConfig(
                ElastiGroup.builder()
                    .id(oldId)
                    .name("foo")
                    .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(4).target(2).build())
                    .build())
            .newElastiGroupOriginalConfig(
                ElastiGroup.builder()
                    .id(newId)
                    .name("foo__Stage__Harness")
                    .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(4).target(2).build())
                    .build())
            .commandRequest(
                SpotInstCommandRequest.builder()
                    .spotInstTaskParameters(SpotInstSetupTaskParameters.builder().timeoutIntervalInMin(10).build())
                    .build())
            .build();
    doReturn(singletonList(element)).when(mockContext).getContextElementList(any());
    Activity activity = Activity.builder().uuid(ACTIVITY_ID).build();
    doReturn(activity)
        .when(mockSpotinstStateHelper)
        .createActivity(any(), any(), anyString(), anyString(), any(), anyList());
    SpotInstCommandRequestBuilder builder = SpotInstCommandRequest.builder();
    doReturn(builder).when(mockSpotinstStateHelper).generateSpotInstCommandRequest(any(), any());
    DelegateTask task = DelegateTask.builder().build();
    doReturn(task)
        .when(mockSpotinstStateHelper)
        .getDelegateTask(anyString(), anyString(), any(), anyString(), anyString(), anyString(), any());
    ExecutionResponse response = state.execute(mockContext);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
    StateExecutionData stateExecutionData = response.getStateExecutionData();
    assertThat(stateExecutionData).isNotNull();
    assertThat(stateExecutionData instanceof SpotInstListenerUpdateStateExecutionData).isTrue();
    SpotInstListenerUpdateStateExecutionData listenerData =
        (SpotInstListenerUpdateStateExecutionData) stateExecutionData;
    SpotInstCommandRequest spotinstCommandRequest = listenerData.getSpotinstCommandRequest();
    assertThat(spotinstCommandRequest).isNotNull();
    SpotInstTaskParameters spotInstTaskParameters = spotinstCommandRequest.getSpotInstTaskParameters();
    assertThat(spotInstTaskParameters).isNotNull();
    assertThat(spotInstTaskParameters instanceof SpotInstSwapRoutesTaskParameters).isTrue();
    SpotInstSwapRoutesTaskParameters swapParams = (SpotInstSwapRoutesTaskParameters) spotInstTaskParameters;
    assertThat(swapParams.isDownsizeOldElastiGroup()).isTrue();
    assertThat(swapParams.getNewElastiGroup().getId()).isEqualTo(newId);
    assertThat(swapParams.getOldElastiGroup().getId()).isEqualTo(oldId);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    SpotInstTaskExecutionResponse delegateResponse =
        SpotInstTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    SpotInstListenerUpdateStateExecutionData stateExecutionData =
        SpotInstListenerUpdateStateExecutionData.builder().build();
    doReturn(stateExecutionData).when(mockContext).getStateExecutionData();
    ExecutionResponse response = state.handleAsyncResponse(mockContext, ImmutableMap.of(ACTIVITY_ID, delegateResponse));
    assertThat(response.getExecutionStatus()).isEqualTo(SUCCESS);
  }
}