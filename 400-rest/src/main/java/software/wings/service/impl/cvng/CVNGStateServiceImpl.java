package software.wings.service.impl.cvng;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.cvng.beans.activity.ActivityVerificationStatus.VERIFICATION_FAILED;
import static io.harness.cvng.beans.activity.ActivityVerificationStatus.VERIFICATION_PASSED;
import static io.harness.cvng.state.CVNGVerificationTask.Status.DONE;

import static software.wings.sm.states.CVNGState.StepStatus.SUCCESS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.client.CVNGService;
import io.harness.cvng.state.CVNGVerificationTask;
import io.harness.cvng.state.CVNGVerificationTaskService;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.service.intfc.cvng.CVNGStateService;
import software.wings.sm.states.CVNGState.CVNGStateResponseData;
import software.wings.sm.states.CVNGState.StepStatus;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@OwnedBy(CV)
public class CVNGStateServiceImpl implements CVNGStateService {
  @Inject private CVNGVerificationTaskService cvngVerificationTaskService;
  @Inject private CVNGService cvngService;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void notifyWorkflowCVNGState(String activityId, StepStatus status) {
    CVNGVerificationTask task = cvngVerificationTaskService.getByActivityId(activityId);
    Preconditions.checkNotNull(task, "Invalid CVNG State found");
    ActivityStatusDTO activityStatusDTO = cvngService.getActivityStatus(task.getAccountId(), activityId);
    activityStatusDTO.setStatus(status == SUCCESS ? VERIFICATION_PASSED : VERIFICATION_FAILED);
    CVNGStateResponseData cvngStateResponseData = CVNGStateResponseData.builder()
                                                      .correlationId(task.getCorrelationId())
                                                      .activityId(activityId)
                                                      .status(DONE)
                                                      .activityStatusDTO(activityStatusDTO)
                                                      .build();
    waitNotifyEngine.doneWith(task.getCorrelationId(), cvngStateResponseData);
    cvngVerificationTaskService.markDone(task.getUuid());
  }
}
