package io.harness.pms.instrumentaion;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.dto.FailureInfoDTO;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.FailureType;
import io.harness.notification.PipelineEventType;
import io.harness.notification.bean.NotificationRules;
import io.harness.notification.bean.PipelineEvent;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.notification.NotificationInstrumentationHelper;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class InstrumentationPipelineEndEventHandlerTest extends CategoryTest {
  @Mock TelemetryReporter telemetryReporter;
  @Mock PMSExecutionService pmsExecutionService;
  @Mock NotificationInstrumentationHelper notificationInstrumentationHelper;
  @InjectMocks InstrumentationPipelineEndEventHandler instrumentationPipelineEndEventHandler;
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testOnEnd() {
    Ambiance ambiance =
        Ambiance.newBuilder()
            .setPlanExecutionId("planExecutionId")
            .putSetupAbstractions(SetupAbstractionKeys.accountId, "accountId")
            .putSetupAbstractions(SetupAbstractionKeys.orgIdentifier, "orgId")
            .putSetupAbstractions(SetupAbstractionKeys.projectIdentifier, "projectId")
            .setMetadata(ExecutionMetadata.newBuilder()
                             .setTriggerInfo(ExecutionTriggerInfo.newBuilder()
                                                 .setTriggeredBy(TriggeredBy.newBuilder()
                                                                     .setIdentifier("admin")
                                                                     .putExtraInfo("email", "admin@harness.io")
                                                                     .build())
                                                 .build())
                             .build())
            .build();

    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity =
        PipelineExecutionSummaryEntity.builder()
            .layoutNodeMap(
                Collections.singletonMap("stage1", GraphLayoutNodeDTO.builder().status(ExecutionStatus.FAILED).build()))
            .executionTriggerInfo(
                ExecutionTriggerInfo.newBuilder().setTriggerType(TriggerType.MANUAL).setIsRerun(false).build())
            .failureInfo(
                FailureInfoDTO.builder()
                    .responseMessages(Collections.singletonList(ResponseMessage.builder().message("message").build()))
                    .failureTypeList(EnumSet.of(FailureType.AUTHENTICATION))
                    .build())
            .startTs(1000L)
            .endTs(2000L)
            .status(ExecutionStatus.FAILED)
            .build();
    List<NotificationRules> notificationRulesList = Collections.singletonList(
        NotificationRules.builder()
            .pipelineEvents(
                Collections.singletonList(PipelineEvent.builder().type(PipelineEventType.PIPELINE_END).build()))
            .build());
    Set<String> notificationMethods = Collections.singleton("slack");
    doReturn(pipelineExecutionSummaryEntity)
        .when(pmsExecutionService)
        .getPipelineExecutionSummaryEntity("accountId", "orgId", "projectId", "planExecutionId", false);
    doReturn(notificationRulesList).when(notificationInstrumentationHelper).getNotificationRules("planExecutionId");
    doReturn(notificationMethods)
        .when(notificationInstrumentationHelper)
        .getNotificationMethodTypes(notificationRulesList);
    instrumentationPipelineEndEventHandler.onEnd(ambiance);

    ArgumentCaptor<HashMap> argumentCaptor = ArgumentCaptor.forClass(HashMap.class);
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(PipelineInstrumentationConstants.PIPELINE_EXECUTION), eq("admin@harness.io"),
            eq("accountId"), argumentCaptor.capture(), any(), any());
    HashMap<String, Object> propertiesMap = argumentCaptor.getValue();
    EnumSet<io.harness.pms.contracts.execution.failure.FailureType> returnedFailureTypes =
        (EnumSet<io.harness.pms.contracts.execution.failure.FailureType>) propertiesMap.get(
            PipelineInstrumentationConstants.FAILURE_TYPES);
    List<String> returnedErrorMessages =
        (List<String>) propertiesMap.get(PipelineInstrumentationConstants.ERROR_MESSAGES);
    Set<String> returnedNotificationMethods =
        (Set<String>) propertiesMap.get(PipelineInstrumentationConstants.NOTIFICATION_METHODS);
    assertEquals(propertiesMap.get(PipelineInstrumentationConstants.LEVEL), StepCategory.PIPELINE);
    assertEquals(propertiesMap.get(PipelineInstrumentationConstants.STATUS), ExecutionStatus.FAILED);
    assertEquals(propertiesMap.get(PipelineInstrumentationConstants.EXECUTION_TIME), 1L);
    assertEquals(returnedNotificationMethods, notificationMethods);
    assertTrue(returnedFailureTypes.contains(FailureType.AUTHENTICATION));
    assertEquals(returnedErrorMessages.get(0), "message");

    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(PipelineInstrumentationConstants.PIPELINE_NOTIFICATION), eq("admin@harness.io"),
            eq("accountId"), argumentCaptor.capture(), any(), any());
    propertiesMap = argumentCaptor.getValue();
    Set<String> eventTypes = (Set<String>) propertiesMap.get(PipelineInstrumentationConstants.EVENT_TYPES);

    assertEquals(eventTypes.size(), 1);
    assertTrue(eventTypes.contains(PipelineEventType.PIPELINE_END));
  }
}
