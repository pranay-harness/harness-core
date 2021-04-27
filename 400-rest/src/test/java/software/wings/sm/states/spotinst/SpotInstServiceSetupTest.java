package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.SATYAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContextImpl;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class SpotInstServiceSetupTest extends WingsBaseTest {
  @Mock private ActivityService mockActivityService;
  @Mock private DelegateService mockDelegateService;
  @Mock private SpotInstStateHelper mockSpotinstStateHelper;
  @Mock private SweepingOutputService mockSweepingOutputService;

  @InjectMocks SpotInstServiceSetup state = new SpotInstServiceSetup("stateName");

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecute() {
    // We should not reply on this flag anymore
    state.setBlueGreen(false);
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    doReturn(OrchestrationWorkflowType.BLUE_GREEN).when(mockContext).getOrchestrationWorkflowType();
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
        .getDelegateTask(
            anyString(), anyString(), any(), anyString(), anyString(), anyString(), any(), any(), anyString());
    state.execute(mockContext);
    verify(mockDelegateService).queueTask(any());
  }

  //  @Test
  //  @Owner(developers = SATYAM)
  //  @Category(UnitTests.class)
  //  public void testHandleAsyncResponse() {
  //    state.setUseCurrentRunningCount(true);
  //    String groupPrefix = "foo";
  //    String newId = "newId";
  //    String oldId = "oldId";
  //    state.setElastiGroupNamePrefix(groupPrefix);
  //    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
  //    Map<String, ResponseData> responseMap = ImmutableMap.of(ACTIVITY_ID,
  //        SpotInstTaskExecutionResponse.builder()
  //            .spotInstTaskResponse(
  //                SpotInstSetupTaskResponse.builder()
  //                    .newElastiGroup(ElastiGroup.builder().id(newId).name("foo__2").build())
  //                    .groupToBeDownsized(singletonList(
  //                        ElastiGroup.builder()
  //                            .id(oldId)
  //                            .name("foo__1")
  //                            .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(2).target(1).build())
  //                            .build()))
  //                    .build())
  //            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
  //            .build());
  //    SpotInstSetupStateExecutionData data =
  //        SpotInstSetupStateExecutionData.builder()
  //            .elastiGroupOriginalConfig(
  //                ElastiGroup.builder()
  //                    .capacity(ElastiGroupCapacity.builder().maximum(0).maximum(4).target(2).build())
  //                    .build())
  //            .spotinstCommandRequest(
  //                SpotInstCommandRequest.builder()
  //                    .spotInstTaskParameters(
  //                        SpotInstSetupTaskParameters.builder().elastiGroupNamePrefix(groupPrefix).build())
  //                    .build())
  //            .build();
  //    doReturn(data).when(mockContext).getStateExecutionData();
  //    doReturn("test").when(mockSpotinstStateHelper).getSweepingOutputName(any(), any());
  //    doReturn(SweepingOutputInstance.builder()).when(mockContext).prepareSweepingOutputBuilder(any());
  //    doReturn(groupPrefix).when(mockContext).renderExpression(anyString());
  //    ExecutionResponse executionResponse = state.handleAsyncResponse(mockContext, responseMap);
  //
  //    verify(mockSweepingOutputService).save(argThat(new ArgumentMatcher<SweepingOutputInstance>() {
  //      @Override
  //      public boolean matches(Object o) {
  //        SweepingOutputInstance sweepingOutputInstance = (SweepingOutputInstance) o;
  //        SpotInstSetupContextElement contextElement = (SpotInstSetupContextElement)
  //        sweepingOutputInstance.getValue(); return
  //        contextElement.getOldElastiGroupOriginalConfig().getName().equals("foo__1")
  //            && contextElement.getNewElastiGroupOriginalConfig().getName().equals("foo__2");
  //      }
  //    }));
  //
  //    assertThat(executionResponse).isNotNull();
  //    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
  //    List<ContextElement> notifyElements = executionResponse.getNotifyElements();
  //    assertThat(notifyElements).isNotNull();
  //    assertThat(notifyElements.size()).isEqualTo(0);
  //  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testValidateFields() {
    state.setMaxInstances("");
    state.setMinInstances("");
    state.setTargetInstances("");
    Map<String, String> fieldMap = state.validateFields();
    assertThat(fieldMap).isNotNull();
    assertThat(fieldMap.size()).isEqualTo(3);
    state.setUseCurrentRunningCount(true);
    fieldMap = state.validateFields();
    assertThat(fieldMap.isEmpty()).isTrue();
  }
}
