package io.harness.cvng.cdng.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.entities.CVNGStepTask.CVNGStepTaskKeys;
import io.harness.cvng.cdng.entities.CVNGStepTask.Status;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
import io.harness.cvng.cdng.services.impl.CVNGStep.CVNGResponseData;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import java.time.Duration;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CVNGStepTaskServiceImplTest extends CvNextGenTestBase {
  @Inject private CVNGStepTaskService cvngStepTaskService;
  @Inject private HPersistence hPersistence;
  @Mock private ActivityService activityService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(cvngStepTaskService, "activityService", activityService, true);
    FieldUtils.writeField(cvngStepTaskService, "waitNotifyEngine", waitNotifyEngine, true);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate() {
    String activityId = generateUuid();
    cvngStepTaskService.create(
        CVNGStepTask.builder().accountId(generateUuid()).activityId(activityId).status(Status.IN_PROGRESS).build());
    assertThat(get(activityId)).isNotNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_invalid() {
    assertThatThrownBy(()
                           -> cvngStepTaskService.create(
                               CVNGStepTask.builder().accountId(generateUuid()).activityId(generateUuid()).build()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testNotifyCVNGStepIfDone() {
    String activityId = generateUuid();
    String accountId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.VERIFICATION_PASSED)
                                              .progressPercentage(100)
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    when(activityService.getActivityStatus(eq(accountId), eq(activityId))).thenReturn(activityStatusDTO);
    cvngStepTaskService.create(
        CVNGStepTask.builder().accountId(accountId).activityId(activityId).status(Status.IN_PROGRESS).build());
    cvngStepTaskService.notifyCVNGStep(get(activityId));
    assertThat(get(activityId).getStatus()).isEqualTo(Status.DONE);
    verify(waitNotifyEngine, times(1))
        .doneWith(eq(activityId),
            eq(CVNGResponseData.builder().activityId(activityId).activityStatusDTO(activityStatusDTO).build()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testNotifyCVNGStep_ifInProgress() {
    String activityId = generateUuid();
    String accountId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.IN_PROGRESS)
                                              .progressPercentage(100)
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    when(activityService.getActivityStatus(eq(accountId), eq(activityId))).thenReturn(activityStatusDTO);
    cvngStepTaskService.create(
        CVNGStepTask.builder().accountId(accountId).activityId(activityId).status(Status.IN_PROGRESS).build());
    cvngStepTaskService.notifyCVNGStep(get(activityId));
    assertThat(get(activityId).getStatus()).isEqualTo(Status.IN_PROGRESS);
    verify(waitNotifyEngine, times(1))
        .doneWith(eq(activityId),
            eq(CVNGResponseData.builder().activityId(activityId).activityStatusDTO(activityStatusDTO).build()));
  }

  private CVNGStepTask get(String activityId) {
    return hPersistence.createQuery(CVNGStepTask.class).filter(CVNGStepTaskKeys.activityId, activityId).get();
  }
}