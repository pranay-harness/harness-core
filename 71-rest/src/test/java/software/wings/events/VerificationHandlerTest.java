package software.wings.events;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.common.VerificationConstants.VERIFICATION_DEPLOYMENTS;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.VerificationEventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VerificationHandlerTest extends WingsBaseTest {
  @Mock HarnessMetricRegistry harnessMetricRegistry;
  @Mock private EventListener eventListener;
  VerificationEventHandler verificationEventHandler;
  @Mock ContinuousVerificationService continuousVerificationService;

  @Before
  public void setup() {
    verificationEventHandler = new VerificationEventHandler(eventListener);
    setInternalState(verificationEventHandler, "harnessMetricRegistry", harnessMetricRegistry);
    setInternalState(verificationEventHandler, "continuousVerificationService", continuousVerificationService);
  }

  @Test
  @Category(UnitTests.class)
  public void testHandleEvent() {
    Map<String, String> properties = new HashMap<>();
    properties.put("accountId", "xyz");
    properties.put("workflowExecutionId", "123");
    properties.put("rollback", "false");
    when(continuousVerificationService.getCVDeploymentData(any(PageRequest.class)))
        .thenReturn(aPageResponse()
                        .withResponse(Arrays.asList(ContinuousVerificationExecutionMetaData.builder()
                                                        .accountId("xyz")
                                                        .workflowExecutionId("123")
                                                        .serviceId("s123")
                                                        .stateType(StateType.NEW_RELIC)
                                                        .executionStatus(ExecutionStatus.SUCCESS)
                                                        .noData(true)
                                                        .build()))
                        .withTotal(1)
                        .build());
    verificationEventHandler.handleEvent(Event.builder()
                                             .eventType(EventType.DEPLOYMENT_VERIFIED)
                                             .eventData(EventData.builder().properties(properties).build())
                                             .build());
    verify(harnessMetricRegistry)
        .recordCounterInc(VERIFICATION_DEPLOYMENTS, "xyz", "s123", "NEW_RELIC", "SUCCESS", "false", "false", "false");
  }

  @Test
  @Category(UnitTests.class)
  public void testHandleEventRollback() {
    Map<String, String> properties = new HashMap<>();
    properties.put("accountId", "xyz");
    properties.put("workflowExecutionId", "123");
    properties.put("rollback", "true");
    when(continuousVerificationService.getCVDeploymentData(any(PageRequest.class)))
        .thenReturn(aPageResponse()
                        .withResponse(Arrays.asList(ContinuousVerificationExecutionMetaData.builder()
                                                        .accountId("xyz")
                                                        .workflowExecutionId("123")
                                                        .serviceId("s123")
                                                        .stateType(StateType.NEW_RELIC)
                                                        .executionStatus(ExecutionStatus.FAILED)
                                                        .noData(false)
                                                        .build()))
                        .withTotal(1)
                        .build());
    verificationEventHandler.handleEvent(Event.builder()
                                             .eventType(EventType.DEPLOYMENT_VERIFIED)
                                             .eventData(EventData.builder().properties(properties).build())
                                             .build());
    verify(harnessMetricRegistry)
        .recordCounterInc(VERIFICATION_DEPLOYMENTS, "xyz", "s123", "NEW_RELIC", "FAILED", "true", "false", "true");
  }
}
