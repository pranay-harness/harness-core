/**
 *
 */

package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.NEW;
import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.STARTING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.queue.Queue;
import io.harness.waiter.WaitNotifyEngine;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
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
public class WorkflowExecutionUpdate implements StateMachineExecutionCallback {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionUpdate.class);
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

  /* (non-Javadoc)
   * @see software.wings.sm.StateMachineExecutionCallback#callback(software.wings.sm.ExecutionContext,
   * software.wings.sm.ExecutionStatus, java.lang.Exception)
   */
  @Override
  public void callback(ExecutionContext context, ExecutionStatus status, Exception ex) {
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter("appId", appId)
                                         .filter(ID_KEY, workflowExecutionId)
                                         .field("status")
                                         .in(asList(NEW, QUEUED, STARTING, RUNNING));

    final long currentTime = System.currentTimeMillis();

    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set("status", status)
                                                        .set("endTs", currentTime);
    wingsPersistence.update(query, updateOps);

    handlePostExecution(context);

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
        triggerService.triggerExecutionPostPipelineCompletionAsync(appId, context.getWorkflowId());
      }
    }
    if (ExecutionStatus.isFinalStatus(status)) {
      alertService.deploymentCompleted(appId, context.getWorkflowExecutionId());
      try {
        WorkflowExecution workflowExecution =
            workflowExecutionService.getWorkflowExecutionSummary(appId, workflowExecutionId);
        alertService.deploymentCompleted(appId, context.getWorkflowExecutionId());
        if (workflowExecution == null) {
          logger.warn("No workflowExecutiod for workflowExecution:[{}], appId:[{}],", workflowExecutionId, appId);
          return;
        }
        eventPublishHelper.handleDeploymentCompleted(workflowExecution);
        String accountID = getApplicationDataForReporting(appId).getAccountId();
        String accountName = accountService.getFromCache(accountID).getAccountName();
        long executionDuration = TimeUnit.MILLISECONDS.toSeconds(currentTime - workflowExecution.getStartTs());
        /**
         * Query workflow execution and project deploymentTrigger, if it is not empty, it is automatic or it is manual
         */
        boolean manual = workflowExecution.getDeploymentTriggerId() == null;

        usageMetricsEventPublisher.publishDeploymentDurationEvent(executionDuration, accountID, accountName);
        usageMetricsEventPublisher.publishDeploymentMetadataEvent(status, manual, accountID, accountName);

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

  private Application getApplicationDataForReporting(String appId) {
    return wingsPersistence.createQuery(Application.class)
        .project(Application.ACCOUNT_ID_KEY, true)
        .filter(Application.ID_KEY, appId)
        .get();
  }
}
