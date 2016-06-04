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

// TODO: Auto-generated Javadoc

/**
 * The Interface WorkflowService.
 *
 * @author Rishi
 */
public interface WorkflowService {
  /**
   * Creates the workflow.
   *
   * @param <T>      the generic type
   * @param cls      the cls
   * @param workflow the workflow
   * @return the t
   */
  <T extends Workflow> T createWorkflow(Class<T> cls, @Valid T workflow);

  /**
   * Update workflow.
   *
   * @param <T>      the generic type
   * @param workflow the workflow
   * @return the t
   */
  <T extends Workflow> T updateWorkflow(@Valid T workflow);

  /**
   * Delete workflow.
   *
   * @param <T>        the generic type
   * @param cls        the cls
   * @param appId      the app id
   * @param workflowId the workflow id
   */
  <T extends Workflow> void deleteWorkflow(Class<T> cls, String appId, String workflowId);

  /**
   * List pipelines.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> req);

  /**
   * Read pipeline.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @return the pipeline
   */
  Pipeline readPipeline(@NotNull String appId, @NotNull String pipelineId);

  /**
   * Update pipeline.
   *
   * @param pipeline the pipeline
   * @return the pipeline
   */
  Pipeline updatePipeline(Pipeline pipeline);

  /**
   * Creates the.
   *
   * @param stateMachine the state machine
   * @return the state machine
   */
  StateMachine create(@Valid StateMachine stateMachine);

  /**
   * Read latest.
   *
   * @param originId the origin id
   * @param name     the name
   * @return the state machine
   */
  StateMachine readLatest(String originId, String name);

  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<StateMachine> list(PageRequest<StateMachine> req);

  /**
   * Trigger.
   *
   * @param appId          the app id
   * @param stateMachineId the state machine id
   * @param executionUuid  the execution uuid
   */
  void trigger(@NotNull String appId, @NotNull String stateMachineId, @NotNull String executionUuid);

  /**
   * Stencils.
   *
   * @param stateTypeScopes the state type scopes
   * @return the map
   */
  Map<StateTypeScope, List<StateTypeDescriptor>> stencils(StateTypeScope... stateTypeScopes);

  /**
   * List orchestration.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Orchestration> listOrchestration(PageRequest<Orchestration> pageRequest);

  /**
   * Read orchestration.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param orchestrationId the orchestration id
   * @return the orchestration
   */
  Orchestration readOrchestration(@NotNull String appId, @NotNull String envId, @NotNull String orchestrationId);

  /**
   * Update orchestration.
   *
   * @param orchestration the orchestration
   * @return the orchestration
   */
  Orchestration updateOrchestration(Orchestration orchestration);

  /**
   * List executions.
   *
   * @param pageRequest  the page request
   * @param includeGraph the include graph
   * @return the page response
   */
  PageResponse<WorkflowExecution> listExecutions(PageRequest<WorkflowExecution> pageRequest, boolean includeGraph);

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
   * @param orchestrationId the orchestration id
   * @param artifactIds     the artifact ids
   * @return the workflow execution
   */
  WorkflowExecution triggerOrchestrationExecution(
      @NotNull String appId, @NotNull String orchestrationId, @NotNull ExecutionArgs executionArgs);

  /**
   * Gets the execution details.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   * @return the execution details
   */
  WorkflowExecution getExecutionDetails(@NotNull String appId, @NotNull String workflowExecutionId);

  WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs);
}
