/**
 *
 */

package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Graph.Node;
import software.wings.sm.ExecutionStatus;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * The Class WorkflowExecution.
 *
 * @author Rishi
 */
@Entity(value = "workflowExecutions", noClassnameStored = true)
public class WorkflowExecution extends Base {
  @Indexed private String workflowId;

  private String stateMachineId;
  @Indexed private String envId;
  private String appName;
  private String envName;
  @Indexed private WorkflowType workflowType;
  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;
  @Transient private Graph graph;
  @Transient private List<String> expandedGroupIds;

  @Transient private Graph.Node executionNode;

  private ErrorStrategy errorStrategy;

  private String name;
  private int total;
  private CountsByStatuses breakdown;

  private ExecutionArgs executionArgs;
  private List<ElementExecutionSummary> serviceExecutionSummaries;
  private LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap;

  private Long startTs;
  private Long endTs;

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets workflow id.
   *
   * @return the workflow id
   */
  public String getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets workflow id.
   *
   * @param workflowId the workflow id
   */
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  /**
   * Gets state machine id.
   *
   * @return the state machine id
   */
  public String getStateMachineId() {
    return stateMachineId;
  }

  /**
   * Sets state machine id.
   *
   * @param stateMachineId the state machine id
   */
  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  /**
   * Gets workflow type.
   *
   * @return the workflow type
   */
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /**
   * Sets workflow type.
   *
   * @param workflowType the workflow type
   */
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  /**
   * Gets graph.
   *
   * @return the graph
   */
  public Graph getGraph() {
    return graph;
  }

  /**
   * Sets graph.
   *
   * @param graph the graph
   */
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets expanded group ids.
   *
   * @return the expanded group ids
   */
  public List<String> getExpandedGroupIds() {
    return expandedGroupIds;
  }

  /**
   * Sets expanded group ids.
   *
   * @param expandedGroupIds the expanded group ids
   */
  public void setExpandedGroupIds(List<String> expandedGroupIds) {
    this.expandedGroupIds = expandedGroupIds;
  }

  /**
   * Getter for property 'total'.
   *
   * @return Value for property 'total'.
   */
  public int getTotal() {
    return total;
  }

  /**
   * Setter for property 'total'.
   *
   * @param total Value to set for property 'total'.
   */
  public void setTotal(int total) {
    this.total = total;
  }

  /**
   * Gets breakdown.
   *
   * @return the breakdown
   */
  public CountsByStatuses getBreakdown() {
    return breakdown;
  }

  /**
   * Sets breakdown.
   *
   * @param breakdown the breakdown
   */
  public void setBreakdown(CountsByStatuses breakdown) {
    this.breakdown = breakdown;
  }

  /**
   * Gets execution args.
   *
   * @return the execution args
   */
  public ExecutionArgs getExecutionArgs() {
    return executionArgs;
  }

  /**
   * Sets execution args.
   *
   * @param executionArgs the execution args
   */
  public void setExecutionArgs(ExecutionArgs executionArgs) {
    this.executionArgs = executionArgs;
  }

  /**
   * Gets service execution summaries.
   *
   * @return the service execution summaries
   */
  public List<ElementExecutionSummary> getServiceExecutionSummaries() {
    return serviceExecutionSummaries;
  }

  /**
   * Sets service execution summaries.
   *
   * @param serviceExecutionSummaries the service execution summaries
   */
  public void setServiceExecutionSummaries(List<ElementExecutionSummary> serviceExecutionSummaries) {
    this.serviceExecutionSummaries = serviceExecutionSummaries;
  }

  /**
   * Gets status instance breakdown map.
   *
   * @return the status instance breakdown map
   */
  public LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> getStatusInstanceBreakdownMap() {
    return statusInstanceBreakdownMap;
  }

  /**
   * Sets status instance breakdown map.
   *
   * @param statusInstanceBreakdownMap the status instance breakdown map
   */
  public void setStatusInstanceBreakdownMap(
      LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap) {
    this.statusInstanceBreakdownMap = statusInstanceBreakdownMap;
  }

  public Node getExecutionNode() {
    return executionNode;
  }

  public void setExecutionNode(Node executionNode) {
    this.executionNode = executionNode;
  }

  /**
   * Is running status boolean.
   *
   * @return the boolean
   */
  public boolean isRunningStatus() {
    return status != null
        && (status == ExecutionStatus.NEW || status == ExecutionStatus.STARTING || status == ExecutionStatus.RUNNING
               || status == ExecutionStatus.ABORTING);
  }

  /**
   * Is failed status boolean.
   *
   * @return the boolean
   */
  public boolean isFailedStatus() {
    return status != null
        && (status == ExecutionStatus.FAILED || status == ExecutionStatus.ABORTED || status == ExecutionStatus.ERROR);
  }

  /**
   * Is paused status boolean.
   *
   * @return the boolean
   */
  public boolean isPausedStatus() {
    return status != null && (status == ExecutionStatus.PAUSED || status == ExecutionStatus.PAUSED_ON_ERROR);
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets error strategy.
   *
   * @return the error strategy
   */
  public ErrorStrategy getErrorStrategy() {
    return errorStrategy;
  }

  /**
   * Sets error strategy.
   *
   * @param errorStrategy the error strategy
   */
  public void setErrorStrategy(ErrorStrategy errorStrategy) {
    this.errorStrategy = errorStrategy;
  }

  /**
   * Gets app name.
   *
   * @return the app name
   */
  public String getAppName() {
    return appName;
  }

  /**
   * Sets app name.
   *
   * @param appName the app name
   */
  public void setAppName(String appName) {
    this.appName = appName;
  }

  /**
   * Gets env name.
   *
   * @return the env name
   */
  public String getEnvName() {
    return envName;
  }

  /**
   * Sets env name.
   *
   * @param envName the env name
   */
  public void setEnvName(String envName) {
    this.envName = envName;
  }
}
