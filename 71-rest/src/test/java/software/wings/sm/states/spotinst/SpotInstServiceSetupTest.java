package software.wings.sm.states.spotinst;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstSetupTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

import java.util.List;
import java.util.Map;

public class SpotInstServiceSetupTest extends WingsBaseTest {
  @Mock private ActivityService mockActivityService;
  @Mock private DelegateService mockDelegateService;
  @Mock private SpotInstStateHelper mockSpotinstStateHelper;

  @InjectMocks SpotInstServiceSetup state = new SpotInstServiceSetup("stateName");

  @Test
  @Category(UnitTests.class)
  public void testExecute() {
    state.setBlueGreen(true);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    SpotInstSetupStateExecutionData data =
        SpotInstSetupStateExecutionData.builder()
            .spotinstCommandRequest(SpotInstCommandRequest.builder()
                                        .spotInstTaskParameters(SpotInstSetupTaskParameters.builder()
                                                                    .image("ami-id")
                                                                    .elastiGroupJson("JSON")
                                                                    .elastiGroupNamePrefix("prefix")
                                                                    .build())
                                        .build())
            .build();
    doReturn(data).when(mockSpotinstStateHelper).prepareStateExecutionData(any(), any());
    DelegateTask task = DelegateTask.builder().build();
    doReturn(task)
        .when(mockSpotinstStateHelper)
        .getDelegateTask(anyString(), anyString(), any(), anyString(), anyString(), anyString(), any(), anyInt());
    state.execute(mockContext);
    verify(mockDelegateService).queueTask(any());
  }

  @Test
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    state.setUseCurrentRunningCount(true);
    String groupPrefix = "foo";
    String newId = "newId";
    String oldId = "oldId";
    state.setElastiGroupNamePrefix(groupPrefix);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    Map<String, ResponseData> responseMap = ImmutableMap.of(ACTIVITY_ID,
        SpotInstTaskExecutionResponse.builder()
            .spotInstTaskResponse(
                SpotInstSetupTaskResponse.builder()
                    .newElastiGroup(ElastiGroup.builder().id(newId).name("foo__2").build())
                    .groupToBeDownsized(singletonList(
                        ElastiGroup.builder()
                            .id(oldId)
                            .name("foo__1")
                            .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(2).target(1).build())
                            .build()))
                    .build())
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .build());
    SpotInstSetupStateExecutionData data =
        SpotInstSetupStateExecutionData.builder()
            .elastiGroupOriginalConfig(
                ElastiGroup.builder()
                    .capacity(ElastiGroupCapacity.builder().maximum(0).maximum(4).target(2).build())
                    .build())
            .build();
    doReturn(data).when(mockContext).getStateExecutionData();
    doReturn(groupPrefix).when(mockContext).renderExpression(anyString());
    ExecutionResponse executionResponse = state.handleAsyncResponse(mockContext, responseMap);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
    List<ContextElement> notifyElements = executionResponse.getNotifyElements();
    assertThat(notifyElements).isNotNull();
    assertThat(notifyElements.size()).isEqualTo(1);
    ContextElement contextElement = notifyElements.get(0);
    assertThat(contextElement).isNotNull();
    assertThat(contextElement instanceof SpotInstSetupContextElement).isTrue();
    SpotInstSetupContextElement spotinstElement = (SpotInstSetupContextElement) contextElement;
    assertThat(spotinstElement.getSpotInstSetupTaskResponse().getNewElastiGroup().getId()).isEqualTo(newId);
    assertThat(spotinstElement.getSpotInstSetupTaskResponse().getGroupToBeDownsized().get(0).getId()).isEqualTo(oldId);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateFields() {
    SpotInstServiceSetup stateLocal = new SpotInstServiceSetup("stateName2");
    stateLocal.setUseCurrentRunningCount(false);
    Map<String, String> fieldMap = stateLocal.validateFields();
    assertThat(fieldMap).isNotNull();
    assertThat(fieldMap.size()).isEqualTo(3);
    stateLocal.setMinInstances(5);
    stateLocal.setTargetInstances(3);
    stateLocal.setMaxInstances(1);
    fieldMap = stateLocal.validateFields();
    assertThat(fieldMap).isNotNull();
    assertThat(fieldMap.size()).isEqualTo(2);
  }
}