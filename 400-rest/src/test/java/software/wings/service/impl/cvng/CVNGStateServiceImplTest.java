package software.wings.service.impl.cvng;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.cvng.state.CVNGVerificationTask.Status.DONE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;

import static software.wings.sm.states.CVNGState.StepStatus.FAILED;
import static software.wings.sm.states.CVNGState.StepStatus.SUCCESS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.client.CVNGService;
import io.harness.cvng.state.CVNGVerificationTask;
import io.harness.cvng.state.CVNGVerificationTaskService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.cvng.CVNGStateService;

import com.google.inject.Inject;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(CV)
public class CVNGStateServiceImplTest extends WingsBaseTest {
  @Inject private CVNGVerificationTaskService cvngVerificationTaskService;
  @Inject private CVNGStateService cvngStateService;
  @Mock private CVNGService cvngService;

  private String accountId;

  @Before
  public void setup() {
    accountId = generateUuid();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testNotifyWorkflowCVNGState_verificationPassed() throws IllegalAccessException {
    String activityId = generateUuid();
    FieldUtils.writeField(cvngStateService, "cvngService", cvngService, true);
    FieldUtils.writeField(cvngStateService, "cvngVerificationTaskService", cvngVerificationTaskService, true);
    String taskId = cvngVerificationTaskService.create(createTask(activityId));
    doReturn(createStatusDTO(accountId)).when(cvngService).getActivityStatus(any(), any());
    cvngStateService.notifyWorkflowCVNGState(activityId, SUCCESS);

    CVNGVerificationTask verificationTask = cvngVerificationTaskService.get(taskId);
    assertThat(verificationTask.getStatus()).isEqualByComparingTo(DONE);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testNotifyWorkflowCVNGState_verificationFailed() throws IllegalAccessException {
    String activityId = generateUuid();
    FieldUtils.writeField(cvngStateService, "cvngService", cvngService, true);
    FieldUtils.writeField(cvngStateService, "cvngVerificationTaskService", cvngVerificationTaskService, true);
    String taskId = cvngVerificationTaskService.create(createTask(activityId));
    doReturn(createStatusDTO(accountId)).when(cvngService).getActivityStatus(any(), any());
    cvngStateService.notifyWorkflowCVNGState(activityId, FAILED);

    CVNGVerificationTask verificationTask = cvngVerificationTaskService.get(taskId);
    assertThat(verificationTask.getStatus()).isEqualByComparingTo(DONE);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testNotifyWorkflowCVNGState_invalidActivity() throws IllegalAccessException {
    String activityId = generateUuid();
    FieldUtils.writeField(cvngStateService, "cvngService", cvngService, true);
    FieldUtils.writeField(cvngStateService, "cvngVerificationTaskService", cvngVerificationTaskService, true);
    doReturn(createStatusDTO(accountId)).when(cvngService).getActivityStatus(any(), any());
    assertThatThrownBy(() -> cvngStateService.notifyWorkflowCVNGState(activityId, SUCCESS))
        .isInstanceOf(NullPointerException.class);
  }

  private CVNGVerificationTask createTask(String activityId) {
    return CVNGVerificationTask.builder()
        .accountId(accountId)
        .activityId(activityId)
        .correlationId(generateUuid())
        .status(CVNGVerificationTask.Status.IN_PROGRESS)
        .build();
  }

  private ActivityStatusDTO createStatusDTO(String accountId) {
    return ActivityStatusDTO.builder().activityId(accountId).build();
  }
}
