/**
 *
 */

package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.service.impl.ExecutionEvent.ExecutionEventBuilder.anExecutionEvent;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.QUEUED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;

import com.google.inject.Inject;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.core.queue.Queue;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.states.EnvState.EnvExecutionResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

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
  @javax.inject.Inject private Queue<ExecutionEvent> executionEventQueue;
  @javax.inject.Inject private AlertService alertService;
  @Inject private TriggerService triggerService;

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
                                         .field("appId")
                                         .equal(appId)
                                         .field(ID_KEY)
                                         .equal(workflowExecutionId)
                                         .field("status")
                                         .in(asList(NEW, QUEUED, STARTING, RUNNING));

    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set("status", status)
                                                        .set("endTs", System.currentTimeMillis());
    wingsPersistence.update(query, updateOps);

    handlePostExecution(context);

    if (!WorkflowType.PIPELINE.equals(context.getWorkflowType())) {
      workflowNotificationHelper.sendWorkflowStatusChangeNotification(context, status);
      if (needToNotifyPipeline) {
        waitNotifyEngine.notify(workflowExecutionId, new EnvExecutionResponseData(workflowExecutionId, status));
      }
      triggerService.triggerExecutionPostPipelineCompletionAsync(appId, context.getWorkflowId());
    }
    if (status.isFinalStatus()) {
      alertService.deploymentCompleted(appId, context.getWorkflowExecutionId());
    }
  }

  protected void handlePostExecution(ExecutionContext context) {
    try {
      WorkflowExecution workflowExecution = workflowExecutionService.getExecutionDetails(appId, workflowExecutionId);
      if (context.getWorkflowType() == WorkflowType.ORCHESTRATION) {
        executionEventQueue.send(
            anExecutionEvent().withAppId(context.getAppId()).withWorkflowId(workflowExecution.getWorkflowId()).build());
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
