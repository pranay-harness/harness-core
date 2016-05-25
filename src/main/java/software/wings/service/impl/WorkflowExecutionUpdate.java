/**
 *
 */
package software.wings.service.impl;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.app.WingsBootstrap;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachineExecutionCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rishi
 *
 */
public class WorkflowExecutionUpdate implements StateMachineExecutionCallback {
  private String appId;
  private String workflowExecutionId;

  public WorkflowExecutionUpdate() {}

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

  @Override
  public void callback(ExecutionContext context, ExecutionStatus status, Exception ex) {
    WingsPersistence wingsPersistence = WingsBootstrap.lookup(WingsPersistence.class);
    List<ExecutionStatus> runningStatuses = new ArrayList<>();
    runningStatuses.add(ExecutionStatus.NEW);
    runningStatuses.add(ExecutionStatus.RUNNING);

    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field("appId")
                                         .equal(appId)
                                         .field("uuid")
                                         .equal(workflowExecutionId)
                                         .field("status")
                                         .in(runningStatuses);

    UpdateOperations<WorkflowExecution> updateOps =
        wingsPersistence.createUpdateOperations(WorkflowExecution.class).set("status", status);
    wingsPersistence.update(query, updateOps);
  }
}
