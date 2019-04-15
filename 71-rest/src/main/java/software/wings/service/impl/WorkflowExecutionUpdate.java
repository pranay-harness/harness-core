/**
 *
 */

package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static java.util.Collections.emptySet;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.queue.Queue;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.states.EnvState.EnvExecutionResponseData;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * The Class WorkflowExecutionUpdate.
 *
 * @author Rishi
 */
@Slf4j
public class WorkflowExecutionUpdate implements StateMachineExecutionCallback {
  private String appId;
  private String workflowExecutionId;
  private boolean needToNotifyPipeline;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private WorkflowNotificationHelper workflowNotificationHelper;
  @Inject private Queue<ExecutionEvent> executionEventQueue;
  @Inject private AlertService alertService;
  @Inject private TriggerService triggerService;
  @Inject private ResourceConstraintService resourceConstraintService;
  @Inject private BarrierService barrierService;
  @Inject private StateExecutionService stateExecutionService;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private UsageMetricsEventPublisher usageMetricsEventPublisher;
  @Inject private AccountService accountService;
  @Inject private WorkflowService workflowService;
  @Inject private UsageMetricsHelper usageMetricsHelper;

  /**
   * Instantiates a new workflow execution update.
   */
  public WorkflowExecutionUpdate() {}

  /**
   * Instantiates a new workflow execution update.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   */
  public WorkflowExecutionUpdate(String appId, String workflowExecutionId) {
    this.appId = appId;
    this.workflowExecutionId = workflowExecutionId;
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets workflow execution id.
   *
   * @return the workflow execution id
   */
  public String getWorkflowExecutionId() {
    return workflowExecutionId;
  }

  /**
   * Sets workflow execution id.
   *
   * @param workflowExecutionId the workflow execution id
   */
  public void setWorkflowExecutionId(String workflowExecutionId) {
    this.workflowExecutionId = workflowExecutionId;
  }

  private static final FindAndModifyOptions callbackFindAndModifyOptions = new FindAndModifyOptions().returnNew(false);

  @Override
  public void callback(ExecutionContext context, ExecutionStatus status, Exception ex) {
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter(WorkflowExecutionKeys.appId, appId)
                                         .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
                                         .field(WorkflowExecutionKeys.startTs)
                                         .exists()
                                         .field(WorkflowExecutionKeys.status)
                                         .in(ExecutionStatus.runningStatuses());

    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set(WorkflowExecutionKeys.status, status)
                                                        .set(WorkflowExecutionKeys.endTs, System.currentTimeMillis());

    // Lets optimistically try to update the workflow status and the end ts, but if we failed, likely the workflow
    // did not even started
    if (wingsPersistence.findAndModify(query, updateOps, callbackFindAndModifyOptions) == null) {
      query = wingsPersistence.createQuery(WorkflowExecution.class)
                  .filter(WorkflowExecutionKeys.appId, appId)
                  .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
                  .field(WorkflowExecutionKeys.status)
                  .in(ExecutionStatus.runningStatuses());

      updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                      .set(WorkflowExecutionKeys.status, status)
                      .unset(WorkflowExecutionKeys.startTs)
                      .unset(WorkflowExecutionKeys.endTs);

      wingsPersistence.update(query, updateOps);
    }

    handlePostExecution(context);

    final String workflowId = context.getWorkflowId();
    if (!WorkflowType.PIPELINE.equals(context.getWorkflowType())) {
      try {
        workflowNotificationHelper.sendWorkflowStatusChangeNotification(context, status);
      } catch (Exception exception) {
        // Failing to send notification is not considered critical to interrupt the status update.
        logger.error("Failed to send notification.", exception);
      }
      if (needToNotifyPipeline) {
        try {
          waitNotifyEngine.notify(workflowExecutionId, new EnvExecutionResponseData(workflowExecutionId, status));
        } catch (WingsException exception) {
          ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
        }
      }
    } else {
      if (status == SUCCESS) {
        triggerService.triggerExecutionPostPipelineCompletionAsync(appId, workflowId);
      }
    }
    if (ExecutionStatus.isFinalStatus(status)) {
      try {
        WorkflowExecution workflowExecution =
            workflowExecutionService.getWorkflowExecutionSummary(appId, workflowExecutionId);
        alertService.deploymentCompleted(appId, context.getWorkflowExecutionId());
        if (workflowExecution == null) {
          logger.warn("No workflowExecution for workflowExecution:[{}], appId:[{}],", workflowExecutionId, appId);
          return;
        }
        eventPublishHelper.handleDeploymentCompleted(workflowExecution);
        if (!WorkflowType.PIPELINE.equals(context.getWorkflowType())) {
          final Application applicationDataForReporting = usageMetricsHelper.getApplication(appId);
          String accountID = applicationDataForReporting.getAccountId();
          String applicationName = applicationDataForReporting.getName();
          String accountName = accountService.getFromCache(accountID).getAccountName();
          long executionDuration = workflowExecution.getStartTs() != null && workflowExecution.getEndTs() != null
              ? TimeUnit.MILLISECONDS.toSeconds(workflowExecution.getEndTs() - workflowExecution.getStartTs())
              : 0;
          /**
           * Query workflow execution and project deploymentTrigger, if it is not empty, it is automatic or it is manual
           */
          boolean manual = workflowExecution.getDeploymentTriggerId() == null;
          String workflowName = usageMetricsHelper.getWorkflowName(context.getAppId(), workflowId);
          usageMetricsEventPublisher.publishDeploymentDurationEvent(
              executionDuration, accountID, accountName, workflowId, workflowName, appId, applicationName);
          usageMetricsEventPublisher.publishDeploymentMetadataEvent(
              status, manual, accountID, accountName, workflowId, workflowName, appId, applicationName);

          if (workflowExecution.getPipelineExecutionId() != null) {
            workflowExecutionService.refreshCollectedArtifacts(
                appId, workflowExecution.getPipelineExecutionId(), workflowExecutionId);
          }
        }
      } catch (Exception e) {
        logger.error(
            "Failed to generate events for workflowExecution:[{}], appId:[{}],", workflowExecutionId, appId, e);
      }
    }
  }

  protected void handlePostExecution(ExecutionContext context) {
    // TODO: this is temporary. this should be part of its own callback and with more precise filter
    try {
      barrierService.updateAllActiveBarriers(context.getAppId());
    } catch (RuntimeException exception) {
      // Do not block the execution for possible exception in the barrier update
      logger.error("Something wrong with barrier update", exception);
    }

    // TODO: this is temporary. this should be part of its own callback and with more precise filter
    try {
      final Set<String> constraintIds =
          resourceConstraintService.updateActiveConstraints(context.getAppId(), workflowExecutionId);

      resourceConstraintService.updateBlockedConstraints(constraintIds);

    } catch (RuntimeException exception) {
      // Do not block the execution for possible exception in the barrier update
      logger.error("Something wrong with resource constraints update", exception);
    }

    try {
      WorkflowExecution workflowExecution =
          workflowExecutionService.getExecutionDetails(appId, workflowExecutionId, true, emptySet());

      if (context.getWorkflowType() == WorkflowType.ORCHESTRATION) {
        executionEventQueue.send(ExecutionEvent.builder()
                                     .appId(appId)
                                     .workflowId(workflowExecution.getWorkflowId())
                                     .infraMappingIds(workflowExecution.getInfraMappingIds())
                                     .build());
      }
    } catch (Exception e) {
      logger.error("Error in breakdown refresh", e);
    }
  }

  public boolean isNeedToNotifyPipeline() {
    return needToNotifyPipeline;
  }

  public void setNeedToNotifyPipeline(boolean needToNotifyPipeline) {
    this.needToNotifyPipeline = needToNotifyPipeline;
  }
}
