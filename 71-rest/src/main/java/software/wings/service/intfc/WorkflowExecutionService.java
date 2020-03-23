package software.wings.service.intfc;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.WorkflowType;
import io.harness.persistence.HIterator;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.beans.ApprovalAuthorization;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.AwsLambdaExecutionSummary;
import software.wings.beans.BuildExecutionSummary;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EnvSummary;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;
import software.wings.beans.HelmExecutionSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.NameValuePair;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.StateExecutionElement;
import software.wings.beans.StateExecutionInterrupt;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.beans.concurrency.ConcurrentExecutionResponse;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.deployment.WorkflowVariablesMetadata;
import software.wings.beans.execution.WorkflowExecutionInfo;
import software.wings.beans.trigger.Trigger;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.WorkflowExecutionUpdate;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.RollbackConfirmation;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateStatusUpdate;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface WorkflowExecutionService extends StateStatusUpdate {
  HIterator<WorkflowExecution> executions(String appId, long startedFrom, long statedTo, Set<String> includeOnlyFields);

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

  WorkflowExecution getExecutionDetails(@NotNull String appId, @NotNull String workflowExecutionId, boolean upToDate);

  WorkflowExecution getExecutionWithoutSummary(@NotNull String appId, @NotNull String workflowExecutionId);

  WorkflowExecution getWorkflowExecution(@NotNull String appId, @NotNull String workflowExecutionId);

  WorkflowExecution getExecutionDetailsWithoutGraph(String appId, String workflowExecutionId);

  WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs, Trigger trigger);

  WorkflowExecution triggerRollbackExecutionWorkflow(String appId, WorkflowExecution workflowExecution);

  RollbackConfirmation getOnDemandRollbackConfirmation(String appId, WorkflowExecution workflowExecution);

  ExecutionInterrupt triggerExecutionInterrupt(@Valid ExecutionInterrupt executionInterrupt);

  void incrementInProgressCount(String appId, String workflowExecutionId, int inc);

  void incrementSuccess(String appId, String workflowExecutionId, int inc);

  void incrementFailed(String appId, String workflowExecutionId, Integer inc);

  RequiredExecutionArgs getRequiredExecutionArgs(String appId, String envId, ExecutionArgs executionArgs);

  WorkflowVariablesMetadata fetchWorkflowVariables(
      String appId, ExecutionArgs executionArgs, String workflowExecutionId);

  DeploymentMetadata fetchDeploymentMetadata(
      String appId, ExecutionArgs executionArgs, boolean withDefaultArtifact, String workflowExecutionId);

  DeploymentMetadata fetchDeploymentMetadata(@NotEmpty String appId, @NotNull ExecutionArgs executionArgs);

  CountsByStatuses getBreakdown(String appId, String workflowExecutionId);

  GraphNode getExecutionDetailsForNode(String appId, String workflowExecutionId, String stateExecutionInstanceId);

  List<StateExecutionData> getExecutionHistory(
      String appId, String workflowExecutionId, String stateExecutionInstanceId);

  List<StateExecutionInterrupt> getExecutionInterrupts(String appId, String stateExecutionInstanceId);

  List<StateExecutionElement> getExecutionElements(String appId, String stateExecutionInstanceId);

  Map<String, GraphGroup> getNodeSubGraphs(
      String appId, String workflowExecutionId, Map<String, List<String>> selectedNodes);

  int getExecutionInterruptCount(String stateExecutionInstanceId);

  StateExecutionInstance getStateExecutionData(String appId, String stateExecutionInstanceId);

  List<StateExecutionInstance> getStateExecutionData(String appId, String executionUuid, String serviceId,
      String infraMappingId, Optional<String> infrastructureDefinitionId, StateType stateType, String stateName);

  List<InfrastructureMapping> getResolvedInfraMappings(Workflow workflow, WorkflowExecution workflowExecution);

  List<InfrastructureDefinition> getResolvedInfraDefinitions(Workflow workflow, WorkflowExecution workflowExecution);

  List<ElementExecutionSummary> getElementsSummary(
      String appId, String executionUuid, String parentStateExecutionInstanceId);

  PhaseExecutionSummary getPhaseExecutionSummary(String appId, String executionUuid, String stateExecutionInstanceId);

  PhaseStepExecutionSummary getPhaseStepExecutionSummary(
      String appId, String executionUuid, String stateExecutionInstanceId);

  boolean workflowExecutionsRunning(WorkflowType workflowType, String appId, String workflowId);

  void updateStartStatus(String appId, String workflowExecutionId, ExecutionStatus status);

  void updateWorkflowElementWithLastGoodReleaseInfo(
      String appId, WorkflowElement workflowElement, String workflowExecutionId);

  boolean updateNotes(String appId, String workflowExecutionId, ExecutionArgs executionArgs);

  boolean approveOrRejectExecution(String appId, List<String> userGroupIds, ApprovalDetails approvalDetails);

  ApprovalStateExecutionData fetchApprovalStateExecutionDataFromWorkflowExecution(
      String appId, String workflowExecutionId, String stateExecutionId, ApprovalDetails approvalDetails);

  List<Artifact> getArtifactsCollected(String appId, String executionUuid);

  List<StateExecutionInstance> getStateExecutionInstances(String appId, String executionUuid);

  List<StateExecutionInstance> getStateExecutionInstancesForPhases(String executionUuid);

  void refreshBuildExecutionSummary(String workflowExecutionId, BuildExecutionSummary buildExecutionSummary);

  Set<WorkflowExecutionBaseline> markBaseline(String appId, String workflowExecutionId, boolean isBaseline);

  WorkflowExecutionBaseline getBaselineDetails(
      String appId, String workflowExecutionId, String stateExecutionId, String currentExecId);

  List<WorkflowExecution> obtainWorkflowExecutions(
      List<String> appIds, long fromDateEpochMilli, String[] projectedKeys);

  List<WorkflowExecution> obtainWorkflowExecutions(String accountId, long fromDateEpochMilli, String[] projectedKeys);

  HIterator<WorkflowExecution> obtainWorkflowExecutionIterator(
      List<String> appIds, long epochMilli, String[] projectedKeys);

  List<Artifact> obtainLastGoodDeployedArtifacts(@NotEmpty String appId, @NotEmpty String workflowId);

  List<ArtifactVariable> obtainLastGoodDeployedArtifactsVariables(String appId, String workflowId);

  WorkflowExecution fetchWorkflowExecution(
      String appId, List<String> serviceIds, List<String> envIds, String workflowId);

  boolean verifyAuthorizedToAcceptOrReject(List<String> userGroupIds, List<String> appIds, String workflowId);

  List<WorkflowExecution> listWaitingOnDeployments(String appId, String workflowExecutionId);

  Long fetchWorkflowExecutionStartTs(String appId, String workflowExecutionId);

  ApprovalAuthorization getApprovalAuthorization(String appId, List<String> userGroupIds);

  WorkflowExecution getWorkflowExecutionSummary(String appId, String workflowExecutionId);
  WorkflowExecution getWorkflowExecutionForVerificationService(String appId, String workflowExecutionId);

  void refreshCollectedArtifacts(String appId, String pipelineExecutionId, String workflowExecutionId);

  StateMachine obtainStateMachine(WorkflowExecution workflowExecution);

  WorkflowExecution fetchLastWorkflowExecution(
      @NotNull String appId, @NotNull String workflowId, String serviceId, String envId);

  PageResponse<WorkflowExecution> fetchWorkflowExecutionList(
      @NotNull String appId, @NotNull String workflowId, @NotNull String envId, int pageOffset, int pageLimit);

  String getApplicationIdByExecutionId(@NotNull String executionId);

  List<WorkflowExecution> getLastSuccessfulWorkflowExecutions(String appId, String workflowId, String serviceId);

  boolean appendInfraMappingId(String appId, String workflowExecutionId, String infraMappingId);

  boolean isTriggerBasedDeployment(ExecutionContext context);

  List<WorkflowExecution> getLatestExecutionsFor(
      String appId, String infraMappingId, int limit, List<String> fieldList, boolean forInclusion);

  void refreshHelmExecutionSummary(String workflowExecutionId, HelmExecutionSummary helmExecutionSummary);

  void refreshAwsLambdaExecutionSummary(
      String workflowExecutionId, List<AwsLambdaExecutionSummary> awsLambdaExecutionSummaries);

  ConcurrentExecutionResponse fetchConcurrentExecutions(
      String appId, String workflowExecutionId, String resourceConstraintName, String unit);

  Map<String, Object> extractServiceInfrastructureDetails(String appId, WorkflowExecution execution);

  int getInstancesDeployedFromExecution(WorkflowExecution workflowExecution);

  List<EnvSummary> getEnvironmentsForExecution(WorkflowExecution workflowExecution);

  List<String> getServiceIdsForExecution(WorkflowExecution workflowExecution);

  List<String> getCloudProviderIdsForExecution(WorkflowExecution workflowExecution);

  List<WorkflowExecution> fetchWorkflowExecutionsForResourceConstraint(String appId, List<String> entityIds);

  boolean getOnDemandRollbackAvailable(String appId, WorkflowExecution lastSuccessfulWE);

  boolean checkIfOnDemand(String appId, String workflowExecutionId);

  WorkflowExecutionInfo getWorkflowExecutionInfo(String workflowExecutionId);

  boolean isMultiService(String appId, String workflowExecutionId);

  void addTagFilterToPageRequest(PageRequest<WorkflowExecution> pageRequest, String tagFilter);

  Map<String, String> getDeploymentTags(String accountId, List<NameValuePair> tags);
}
