package software.wings.service.intfc;

import software.wings.beans.CountsByStatuses;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Graph.Node;
import software.wings.beans.Graph.NodeOps;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.sm.ExecutionEvent;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * The Interface WorkflowService.
 *
 * @author Rishi
 */
public interface WorkflowExecutionService {
  /**
   * /**
   * Trigger.
   *
   * @param appId          the app id
   * @param stateMachineId the state machine id
   * @param executionUuid  the execution uuid
   * @param executionName  the execution name
   */
  void trigger(
      @NotNull String appId, @NotNull String stateMachineId, @NotNull String executionUuid, String executionName);

  /**
   * List executions.
   *
   * @param pageRequest  the page request
   * @param includeGraph the include graph
   * @return the page response
   */
  PageResponse<WorkflowExecution> listExecutions(PageRequest<WorkflowExecution> pageRequest, boolean includeGraph);

  /**
   * List executions page response.
   *
   * @param pageRequest             the page request
   * @param includeGraph            the include graph
   * @param runningOnly             the running only
   * @param withBreakdownAndSummary the with breakdown and summary
   * @return the page response
   */
  PageResponse<WorkflowExecution> listExecutions(PageRequest<WorkflowExecution> pageRequest, boolean includeGraph,
      boolean runningOnly, boolean withBreakdownAndSummary);

  /**
   * Trigger pipeline execution.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @return the workflow execution
   */
  WorkflowExecution triggerPipelineExecution(@NotNull String appId, @NotNull String pipelineId);

  /**
   * Trigger orchestration execution.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param orchestrationId the orchestration id
   * @param executionArgs   the execution args
   * @return the workflow execution
   */
  WorkflowExecution triggerOrchestrationExecution(@NotNull String appId, @NotNull String envId,
      @NotNull String orchestrationId, @NotNull ExecutionArgs executionArgs);

  /**
   * Gets the execution details.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   * @return the execution details
   */
  WorkflowExecution getExecutionDetails(@NotNull String appId, @NotNull String workflowExecutionId);

  /**
   * Gets the execution details.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   * @param expandedGroupIds    the expanded group ids
   * @param requestedGroupId    the requested group id
   * @param nodeOps             the node ops
   * @return the execution details
   */
  WorkflowExecution getExecutionDetails(@NotNull String appId, @NotNull String workflowExecutionId,
      List<String> expandedGroupIds, String requestedGroupId, NodeOps nodeOps);

  /**
   * Gets execution details without graph.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   * @return the execution details without graph
   */
  WorkflowExecution getExecutionDetailsWithoutGraph(String appId, String workflowExecutionId);

  /**
   * Trigger env execution workflow execution.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param executionArgs the execution args
   * @return the workflow execution
   */
  WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs);

  /**
   * Trigger execution event
   *
   * @param executionEvent the workflow execution event
   * @return execution event
   */
  ExecutionEvent triggerExecutionEvent(@Valid ExecutionEvent executionEvent);

  /**
   * Increment in progress count.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   * @param inc                 the inc
   */
  void incrementInProgressCount(String appId, String workflowExecutionId, int inc);

  /**
   * Increment success.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   * @param inc                 the inc
   */
  void incrementSuccess(String appId, String workflowExecutionId, int inc);

  /**
   * Increment failed.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   * @param inc                 the inc
   */
  void incrementFailed(String appId, String workflowExecutionId, Integer inc);

  /**
   * Gets required execution args.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param executionArgs the execution args
   * @return the required execution args
   */
  RequiredExecutionArgs getRequiredExecutionArgs(String appId, String envId, ExecutionArgs executionArgs);

  /**
   * Gets breakdown.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   * @return the breakdown
   */
  CountsByStatuses getBreakdown(String appId, String workflowExecutionId);

  /**
   * Gets execution details for node.
   *
   * @param appId                    the app id
   * @param workflowExecutionId      the workflow execution id
   * @param stateExecutionInstanceId the state execution instance id
   * @return the execution details for node
   */
  Node getExecutionDetailsForNode(String appId, String workflowExecutionId, String stateExecutionInstanceId);
}
