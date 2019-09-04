package software.wings.service.impl.verification;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.service.intfc.verification.CVActivityLogService.Logger;
import software.wings.service.intfc.verification.CVTaskService;
import software.wings.verification.CVTask;
import software.wings.verification.CVTask.CVTaskKeys;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
@Slf4j
public class CVTaskServiceImpl implements CVTaskService {
  public static final int CV_TASK_TIMEOUT_MINUTES = 15;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private CVActivityLogService activityLogService;
  @Inject Clock clock;
  // This is only available at manager side and not accessible from verification.
  @Inject(optional = true) @Nullable private WaitNotifyEngine waitNotifyEngine;

  @Override
  public CVTask enqueueTask(String accountId, String cvConfigId, long startMilliSec, long endMilliSec) {
    CVTask cvTask = CVTask.builder()
                        .accountId(accountId)
                        .cvConfigId(cvConfigId)
                        .status(ExecutionStatus.SUCCESS) // TODO: Change this to queued. marking this passed for the
                                                         // first release. This is a migration strategy to fill cvTasks.
                        .build();
    wingsPersistence.save(cvTask);
    return cvTask;
  }

  @Override
  public void saveCVTask(CVTask cvTask) {
    wingsPersistence.save(cvTask);
  }

  @Override
  public void enqueueSequentialTasks(List<CVTask> cvTasks) {
    CVTask lastCVTask = null;
    for (CVTask cvTask : cvTasks) {
      cvTask.setStatus(ExecutionStatus.WAITING);
      this.saveCVTask(cvTask); // assigns uuid
      if (lastCVTask != null) {
        lastCVTask.setNextTaskId(cvTask.getUuid());
      }
      lastCVTask = cvTask;
    }
    if (cvTasks.size() > 0) {
      cvTasks.get(0).setStatus(ExecutionStatus.QUEUED);
    }
    cvTasks.forEach(cvTask -> this.saveCVTask(cvTask));
  }

  @Override
  public Optional<CVTask> getNextTask(String accountId) {
    CVTask cvTask = wingsPersistence.createQuery(CVTask.class)
                        .filter(CVTaskKeys.accountId, accountId)
                        .filter(CVTaskKeys.status, ExecutionStatus.QUEUED)
                        .filter(CVTaskKeys.validAfter + " <=", clock.millis())
                        .order(CVTaskKeys.lastUpdatedAt)
                        .get();
    if (cvTask == null) {
      return Optional.empty();
    }
    cvTask.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.updateField(CVTask.class, cvTask.getUuid(), CVTaskKeys.status, ExecutionStatus.RUNNING);
    // TODO: add parallel execution support
    return Optional.of(cvTask);
  }

  @Override
  public CVTask getCVTask(String cvTaskId) {
    return wingsPersistence.get(CVTask.class, cvTaskId);
  }

  @Override
  public void updateTaskStatus(String cvTaskId, DataCollectionTaskResult result) {
    CVTask cvTask = getCVTask(cvTaskId);
    Logger activityLogger = getActivityLogger(cvTask);
    activityLogger.info("Data collection task completed with status: " + result.getStatus() + ". minute: %t",
        cvTask.getDataCollectionInfo().getStartTime().toEpochMilli());
    if (result.getStatus() == DataCollectionTaskStatus.FAILURE) {
      activityLogger.error("Data collection task failed with exception " + result.getErrorMessage() + ". minute: %t",
          cvTask.getDataCollectionInfo().getStartTime().toEpochMilli());
      Map<String, Object> updateMap =
          ImmutableMap.of(CVTaskKeys.status, ExecutionStatus.FAILED, CVTaskKeys.exception, result.getErrorMessage());
      wingsPersistence.updateFields(CVTask.class, cvTaskId, updateMap);
      cvTask.setException(result.getErrorMessage());
      cvTask.setStatus(ExecutionStatus.FAILED);
      markWaitingTasksFailed(cvTask);
      markStateFailed(cvTask);
      sendNotificationIfRequired(cvTask);
    } else if (result.getStatus() == DataCollectionTaskStatus.SUCCESS) {
      wingsPersistence.updateField(CVTask.class, cvTaskId, CVTaskKeys.status, ExecutionStatus.SUCCESS);
      enqueueNextTask(cvTask);
    } else {
      throw new RuntimeException("Not implemented: " + result.getStatus());
    }
  }

  public void enqueueNextTask(CVTask cvTask) {
    if (cvTask.getNextTaskId() != null) {
      logger.info("Enqueuing next task {}", cvTask.getUuid());
      wingsPersistence.updateField(CVTask.class, cvTask.getNextTaskId(), CVTaskKeys.status, ExecutionStatus.QUEUED);
    }
  }

  @Override
  public void expireLongRunningTasks(String accountId) {
    logger.info("Running expire long running tasks for accountId {}", accountId);
    UpdateOperations<CVTask> updateOperations = wingsPersistence.createUpdateOperations(CVTask.class)
                                                    .set(CVTaskKeys.status, ExecutionStatus.EXPIRED)
                                                    .set(CVTaskKeys.exception, "Task timed out");
    Query<CVTask> query = wingsPersistence.createQuery(CVTask.class)
                              .filter(CVTaskKeys.status, ExecutionStatus.RUNNING)
                              .filter(CVTaskKeys.lastUpdatedAt + " <=",
                                  clock.instant().minus(CV_TASK_TIMEOUT_MINUTES, ChronoUnit.MINUTES).toEpochMilli());
    UpdateResults updateResults = wingsPersistence.update(query, updateOperations);
    if (updateResults.getUpdatedCount() != 0) {
      findByExecutionStatus(accountId, ExecutionStatus.EXPIRED).forEach(cvTask -> {
        markWaitingTasksFailed(cvTask);
        wingsPersistence.updateField(CVTask.class, cvTask.getUuid(), CVTaskKeys.status, ExecutionStatus.FAILED);
      });
    }
  }

  @Override
  public void retryTasks(String accountId) {
    // TODO
  }

  private void sendNotificationIfRequired(CVTask cvTask) {
    // TODO: send notification in case of 24 * 7 task failure
  }

  private void markStateFailed(CVTask cvTask) {
    if (cvTask.getStateExecutionId() != null) {
      logger.info("Marking stateExecutionId {} as failed", cvTask.getStateExecutionId());
      VerificationStateAnalysisExecutionData analysisExecutionData =
          VerificationStateAnalysisExecutionData.builder()
              .correlationId(cvTask.getCorrelationId())
              .stateExecutionInstanceId(cvTask.getStateExecutionId())
              .build();
      analysisExecutionData.setStatus(ExecutionStatus.ERROR);
      analysisExecutionData.setErrorMsg(cvTask.getException());
      VerificationDataAnalysisResponse verificationDataAnalysisResponse =
          VerificationDataAnalysisResponse.builder().stateExecutionData(analysisExecutionData).build();
      verificationDataAnalysisResponse.setExecutionStatus(ExecutionStatus.ERROR);
      waitNotifyEngine.notify(analysisExecutionData.getCorrelationId(), verificationDataAnalysisResponse);
    }
  }

  private void markWaitingTasksFailed(CVTask cvTask) {
    if (cvTask.getStateExecutionId() != null) {
      String exceptionMsg =
          cvTask.getStatus() == ExecutionStatus.EXPIRED ? "Previous task timed out" : "Previous task failed";
      logger.info("Marking queued task failed for stateExecutionId {}", cvTask.getStateExecutionId());
      // TODO: this is simple logic with no retry logic for now. This might change based on on retry logic.
      UpdateOperations<CVTask> updateOperations = wingsPersistence.createUpdateOperations(CVTask.class)
                                                      .set(CVTaskKeys.status, ExecutionStatus.FAILED)
                                                      .set(CVTaskKeys.exception, exceptionMsg);
      Query<CVTask> query =
          wingsPersistence.createQuery(CVTask.class).filter(CVTaskKeys.stateExecutionId, cvTask.getStateExecutionId());
      query.or(query.criteria(CVTaskKeys.status).equal(ExecutionStatus.QUEUED),
          query.criteria(CVTaskKeys.status).equal(ExecutionStatus.WAITING));

      wingsPersistence.update(query, updateOperations);
    }
  }

  private Logger getActivityLogger(CVTask cvTask) {
    return activityLogService.getLogger(cvTask.getDataCollectionInfo().getCvConfigId(),
        cvTask.getDataCollectionInfo().getEndTime().toEpochMilli(), cvTask.getStateExecutionId());
  }

  public List<CVTask> findByExecutionStatus(String accountId, ExecutionStatus executionStatus) {
    PageRequest<CVTask> pageRequest = aPageRequest()
                                          .addFilter(CVTaskKeys.accountId, Operator.EQ, accountId)
                                          .addFilter(CVTaskKeys.status, Operator.EQ, executionStatus)
                                          .build();
    return wingsPersistence.query(CVTask.class, pageRequest).getResponse();
  }
}
