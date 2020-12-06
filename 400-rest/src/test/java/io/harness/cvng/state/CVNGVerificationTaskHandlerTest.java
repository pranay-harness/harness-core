package io.harness.cvng.state;

import static io.harness.cvng.state.CVNGVerificationTask.Status.DONE;
import static io.harness.cvng.state.CVNGVerificationTask.Status.IN_PROGRESS;
import static io.harness.cvng.state.CVNGVerificationTask.Status.TIMED_OUT;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.client.CVNGService;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.WingsBaseTest;
import software.wings.sm.states.CVNGState.CVNGStateResponseData;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class CVNGVerificationTaskHandlerTest extends WingsBaseTest {
  @Inject private CVNGVerificationTaskHandler cvngVerificationTaskHandler;
  @Inject private CVNGVerificationTaskService cvngVerificationTaskService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private CVNGService cvngService;
  private Clock clock;
  private String accountId;
  private String activityId;
  private String correlationId;
  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    accountId = generateUuid();
    activityId = generateUuid();
    correlationId = generateUuid();
    clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    FieldUtils.writeField(cvngVerificationTaskHandler, "clock", clock, true);
    FieldUtils.writeField(cvngVerificationTaskHandler, "cvngService", cvngService, true);
    FieldUtils.writeField(cvngVerificationTaskHandler, "waitNotifyEngine", waitNotifyEngine, true);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandle_notifyProgressDontCallWaitNotify() {
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.IN_PROGRESS)
                                              .progressPercentage(10)
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    when(cvngService.getActivityStatus(eq(accountId), eq(activityId))).thenReturn(activityStatusDTO);
    CVNGVerificationTask cvngVerificationTask = create();
    cvngVerificationTaskService.create(cvngVerificationTask);
    cvngVerificationTaskHandler.handle(cvngVerificationTask);
    verifyZeroInteractions(waitNotifyEngine);
    assertThat(cvngVerificationTaskService.get(cvngVerificationTask.getUuid()).getStatus()).isEqualTo(IN_PROGRESS);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandle_notifyFinalStatus() {
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.VERIFICATION_PASSED)
                                              .progressPercentage(100)
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    when(cvngService.getActivityStatus(eq(accountId), eq(activityId))).thenReturn(activityStatusDTO);
    CVNGVerificationTask cvngVerificationTask = create();
    cvngVerificationTaskService.create(cvngVerificationTask);
    cvngVerificationTaskHandler.handle(cvngVerificationTask);
    ArgumentCaptor<CVNGStateResponseData> argumentCaptor = ArgumentCaptor.forClass(CVNGStateResponseData.class);
    verify(waitNotifyEngine).doneWith(eq(correlationId), argumentCaptor.capture());
    CVNGStateResponseData cvngStateResponseData = argumentCaptor.getValue();
    assertThat(cvngStateResponseData.getActivityId()).isEqualTo(activityId);
    assertThat(cvngStateResponseData.getCorrelationId()).isEqualTo(correlationId);
    assertThat(cvngStateResponseData.getStatus()).isEqualTo(DONE);
    assertThat(cvngStateResponseData.getActivityStatusDTO()).isEqualTo(activityStatusDTO);
    assertThat(cvngVerificationTaskService.get(cvngVerificationTask.getUuid()).getStatus()).isEqualTo(DONE);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandle_timeout() {
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.VERIFICATION_PASSED)
                                              .progressPercentage(100)
                                              .durationMs(Duration.ofMinutes(10).toMillis())
                                              .build();
    when(cvngService.getActivityStatus(eq(accountId), eq(activityId))).thenReturn(activityStatusDTO);
    CVNGVerificationTask cvngVerificationTask = create();
    cvngVerificationTaskService.create(cvngVerificationTask);
    cvngVerificationTaskHandler.handle(cvngVerificationTask);
    ArgumentCaptor<CVNGStateResponseData> argumentCaptor = ArgumentCaptor.forClass(CVNGStateResponseData.class);
    verify(waitNotifyEngine).doneWith(eq(correlationId), argumentCaptor.capture());
    CVNGStateResponseData cvngStateResponseData = argumentCaptor.getValue();
    assertThat(cvngStateResponseData.getActivityId()).isEqualTo(activityId);
    assertThat(cvngStateResponseData.getCorrelationId()).isEqualTo(correlationId);
    assertThat(cvngStateResponseData.getStatus()).isEqualTo(TIMED_OUT);
    assertThat(cvngStateResponseData.getActivityStatusDTO()).isEqualTo(activityStatusDTO);
    assertThat(cvngStateResponseData.getExecutionStatus()).isEqualTo(ExecutionStatus.EXPIRED);
    assertThat(cvngVerificationTaskService.get(cvngVerificationTask.getUuid()).getStatus()).isEqualTo(TIMED_OUT);
  }

  private CVNGVerificationTask create() {
    return CVNGVerificationTask.builder()
        .accountId(accountId)
        .activityId(activityId)
        .correlationId(correlationId)
        .startTime(clock.instant().minus(Duration.ofMinutes(45)))
        .status(IN_PROGRESS)
        .build();
  }
}