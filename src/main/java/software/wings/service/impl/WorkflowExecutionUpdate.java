/**
 *
 */

package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.api.SimpleWorkflowParam;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachineExecutionCallback;

import java.util.ArrayList;
import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * The Class WorkflowExecutionUpdate.
 *
 * @author Rishi
 */
public class WorkflowExecutionUpdate implements StateMachineExecutionCallback {
  private String appId;
  private String workflowExecutionId;

  @Inject private WingsPersistence wingsPersistence;

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
    runningStatuses.add(ExecutionStatus.RUNNING);

    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field("appId")
                                         .equal(appId)
                                         .field(ID_KEY)
                                         .equal(workflowExecutionId)
                                         .field("status")
                                         .in(runningStatuses);

    UpdateOperations<WorkflowExecution> updateOps =
        wingsPersistence.createUpdateOperations(WorkflowExecution.class).set("status", status);

    if (!context.getContextElementList(ContextElementType.PARAM)
             .stream()
             .filter(contextElement -> contextElement instanceof SimpleWorkflowParam)
             .findFirst()
             .isPresent()) {
      String breakdownKey = "breakdown.";
      switch (status) {
        case SUCCESS:
          breakdownKey += "success";
          break;
        case FAILED:
        case ERROR:
          breakdownKey += "failed";
          break;
      }
      updateOps.set(breakdownKey, 1);
      updateOps.set("breakdown.inprogress", 0);
    }
    wingsPersistence.update(query, updateOps);
  }
}
