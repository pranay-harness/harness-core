package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.HIterator;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.BuildExecutionSummary;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.StateExecutionElement;
import software.wings.beans.StateExecutionInterrupt;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.beans.trigger.Trigger;
import software.wings.service.impl.WorkflowExecutionUpdate;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateStatusUpdate;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface WorkflowExecutionService extends StateStatusUpdate {
  void trigger(
      @NotNull String appId, @NotNull String stateMachineId, @NotNull String executionUuid, String executionName);

  PageResponse<WorkflowExecution> listExecutions(PageRequest<WorkflowExecution> pageRequest, boolean includeGraph);

  PageResponse<WorkflowExecution> listExecutions(PageRequest<WorkflowExecution> pageRequest, boolean includeGraph,
      boolean runningOnly, boolean withBreakdownAndSummary, boolean includeStatus);

  WorkflowExecution triggerPipelineExecution(
      @NotNull String appId, @NotNull String pipelineId, ExecutionArgs executionArgs, Trigger trigger);

  WorkflowExecution triggerOrchestrationExecution(@NotNull String appId, String envId, @NotNull String orchestrationId,
      @NotNull ExecutionArgs executionArgs, Trigger trigger);

  WorkflowExecution triggerOrchestrationExecution(@NotNull String appId, String envId, @NotNull String orchestrationId,
      String pipelineExecutionId, @NotNull ExecutionArgs executionArgs, Trigger trigger);

  WorkflowExecution triggerOrchestrationWorkflowExecution(String appId, String envId, String orchestrationId,
      String pipelineExecutionId, @NotNull ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate,
      Trigger trigger);

  WorkflowExecution getExecutionDetails(
      @NotNull String appId, @NotNull String workflowExecutionId, boolean upToDate, Set<String> excludeFromAggregation);

  WorkflowExecution getExecutionWithoutSummary(@NotNull String appId, @NotNull String workflowExecutionId);

  WorkflowExecution getWorkflowExecution(@NotNull String appId, @NotNull String workflowExecutionId);

  WorkflowExecution getExecutionDetailsWithoutGraph(String appId, String workflowExecutionId);

  WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs, Trigger trigger);

  ExecutionInterrupt triggerExecutionInterrupt(@Valid ExecutionInterrupt executionInterrupt);

  void incrementInProgressCount(String appId, String workflowExecutionId, int inc);

  void incrementSuccess(String appId, String workflowExecutionId, int inc);

  void incrementFailed(String appId, String workflowExecutionId, Integer inc);

  RequiredExecutionArgs getRequiredExecutionArgs(String appId, String envId, ExecutionArgs executionArgs);

  CountsByStatuses getBreakdown(String appId, String workflowExecutionId);

  GraphNode getExecutionDetailsForNode(String appId, String workflowExecutionId, String stateExecutionInstanceId);

  List<StateExecutionData> getExecutionHistory(
      String appId, String workflowExecutionId, String stateExecutionInstanceId);

  List<StateExecutionInterrupt> getExecutionInterrupts(String appId, String stateExecutionInstanceId);

  List<StateExecutionElement> getExecutionElements(String appId, String stateExecutionInstanceId);

  int getExecutionInterruptCount(String stateExecutionInstanceId);

  StateExecutionInstance getStateExecutionData(String appId, String stateExecutionInstanceId);

  List<StateExecutionInstance> getStateExecutionData(String appId, String executionUuid, String serviceId,
      String infraMappingId, StateType stateType, String stateName);

  List<InfrastructureMapping> getResolvedInfraMappings(Workflow workflow, WorkflowExecution workflowExecution);

  List<ElementExecutionSummary> getElementsSummary(
      String appId, String executionUuid, String parentStateExecutionInstanceId);

  PhaseExecutionSummary getPhaseExecutionSummary(String appId, String executionUuid, String stateExecutionInstanceId);

  PhaseStepExecutionSummary getPhaseStepExecutionSummary(
      String appId, String executionUuid, String stateExecutionInstanceId);

  boolean workflowExecutionsRunning(WorkflowType workflowType, String appId, String workflowId);

  boolean updateNotes(String appId, String workflowExecutionId, ExecutionArgs executionArgs);

  boolean approveOrRejectExecution(String appId, List<String> userGroupIds, ApprovalDetails approvalDetails);

  ApprovalStateExecutionData fetchApprovalStateExecutionDataFromWorkflowExecution(
      String appId, String workflowExecutionId, String stateExecutionId, ApprovalDetails approvalDetails);

  List<Artifact> getArtifactsCollected(String appId, String executionUuid);

  void refreshBuildExecutionSummary(
      String appId, String workflowExecutionId, BuildExecutionSummary buildExecutionSummary);

  Set<WorkflowExecutionBaseline> markBaseline(String appId, String workflowExecutionId, boolean isBaseline);

  WorkflowExecutionBaseline getBaselineDetails(
      String appId, String workflowExecutionId, String stateExecutionId, String currentExecId);

  List<WorkflowExecution> obtainWorkflowExecutions(List<String> appIds, long fromDateEpochMilli);

  HIterator<WorkflowExecution> obtainWorkflowExecutionIterator(List<String> appIds, long epochMilli);

  List<Artifact> obtainLastGoodDeployedArtifacts(@NotEmpty String appId, @NotEmpty String workflowId);

  WorkflowExecution fetchWorkflowExecution(
      String appId, List<String> serviceIds, List<String> envIds, String workflowId);

  boolean verifyAuthorizedToAcceptOrReject(List<String> userGroupIds, List<String> appIds, String workflowId);
}
