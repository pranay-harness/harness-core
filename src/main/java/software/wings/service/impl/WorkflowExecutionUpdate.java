/**
 *
 */
package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
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

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getWorkflowExecutionId() {
    return workflowExecutionId;
  }

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
    wingsPersistence.update(query, updateOps);
  }
}
