package software.wings.service.intfc;

import software.wings.beans.ExecutionArgs;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author Rishi
 */
public interface WorkflowService {
  <T extends Workflow> T createWorkflow(Class<T> cls, @Valid T workflow);

  <T extends Workflow> T updateWorkflow(@Valid T workflow);

  <T extends Workflow> void deleteWorkflow(Class<T> cls, String appId, String workflowId);

  PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> req);

  Pipeline readPipeline(@NotNull String appId, @NotNull String pipelineId);

  Pipeline updatePipeline(Pipeline pipeline);

  StateMachine create(@Valid StateMachine stateMachine);

  StateMachine readLatest(String originId, String name);

  PageResponse<StateMachine> list(PageRequest<StateMachine> req);

  void trigger(@NotNull String appId, @NotNull String stateMachineId, @NotNull String executionUuid);

  Map<StateTypeScope, List<StateTypeDescriptor>> stencils(StateTypeScope... stateTypeScopes);

  PageResponse<Orchestration> listOrchestration(PageRequest<Orchestration> pageRequest);

  Orchestration readOrchestration(@NotNull String appId, @NotNull String envId, @NotNull String orchestrationId);

  Orchestration updateOrchestration(Orchestration orchestration);

  PageResponse<WorkflowExecution> listExecutions(PageRequest<WorkflowExecution> pageRequest, boolean includeGraph);

  WorkflowExecution triggerPipelineExecution(@NotNull String appId, @NotNull String pipelineId);

  WorkflowExecution triggerOrchestrationExecution(
      @NotNull String appId, @NotNull String orchestrationId, @NotNull ExecutionArgs executionArgs);

  WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs);

  WorkflowExecution getExecutionDetails(@NotNull String appId, @NotNull String workflowExecutionId);
}
