/**
 *
 */

package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.states.EnvState.EnvExecutionResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class WorkflowExecutionUpdate.
 *
 * @author Rishi
 */
public class WorkflowExecutionUpdate implements StateMachineExecutionCallback {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionUpdate.class);
  private String appId;
  private String workflowExecutionId;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private PipelineService pipelineService;

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
    List<ExecutionStatus> runningStatuses = new ArrayList<>();
    runningStatuses.add(ExecutionStatus.NEW);
    runningStatuses.add(ExecutionStatus.STARTING);
    runningStatuses.add(ExecutionStatus.RUNNING);

    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field("appId")
                                         .equal(appId)
                                         .field(ID_KEY)
                                         .equal(workflowExecutionId)
                                         .field("status")
                                         .in(runningStatuses);

    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set("status", status)
                                                        .set("endTs", System.currentTimeMillis());
    wingsPersistence.update(query, updateOps);

    if (context.getWorkflowType() != null && context.getWorkflowType().equals(WorkflowType.PIPELINE)) {
      pipelineService.updatePipelineExecutionData(appId, workflowExecutionId, status);
    }

    if (!isNullOrEmpty(workflowExecutionId)) { // TODO:: remove this check.
      waitNotifyEngine.notify(workflowExecutionId, new EnvExecutionResponseData(workflowExecutionId, status));
    }

    try {
      workflowExecutionService.getExecutionDetails(appId, workflowExecutionId);
    } catch (Exception e) {
      logger.error("Error in breakdown refresh", e);
    }
  }
}
