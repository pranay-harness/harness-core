package software.wings.service.intfc;

import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowFailureStrategy;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.Stencil;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  <T extends Workflow> T updateWorkflow(@Valid T workflow, Integer version);

  /**
   * Delete workflow.
   *
   * @param <T>        the generic type
   * @param cls        the cls
   * @param appId      the app id
   * @param workflowId the workflow id
   * @return the boolean
   */
  <T extends Workflow> boolean deleteWorkflow(Class<T> cls, String appId, String workflowId);

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
   * @param appId    the app id
   * @param originId the origin id
   * @return the state machine
   */
  StateMachine readLatest(String appId, String originId);

  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<StateMachine> list(PageRequest<StateMachine> req);

  /**
   * Stencils.
   *
   * @param appId           the app id
   * @param stateTypeScopes the state type scopes
   * @return the map
   */
  Map<StateTypeScope, List<Stencil>> stencils(String appId, StateTypeScope... stateTypeScopes);

  /**
   * List orchestration.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Orchestration> listOrchestration(PageRequest<Orchestration> pageRequest, String envId);

  /**
   * Read orchestration.
   *
   * @param appId           the app id
   * @param orchestrationId the orchestration id
   * @return the orchestration
   */
  Orchestration readOrchestration(@NotNull String appId, @NotNull String orchestrationId);

  /**
   * Update orchestration.
   *
   * @param orchestration the orchestration
   * @return the orchestration
   */
  Orchestration updateOrchestration(Orchestration orchestration);

  /**
   * Read latest simple workflow orchestration.
   *
   * @param appId the app id
   * @return the orchestration
   */
  Orchestration readLatestSimpleWorkflow(String appId);
  /**
   * Delete workflow by environment.
   *
   * @param appId the app id
   */
  void deleteWorkflowByApplication(String appId);

  /**
   * Delete state machines my application.
   *
   * @param appId the app id
   */
  void deleteStateMachinesByApplication(String appId);

  List<WorkflowFailureStrategy> listWorkflowFailureStrategies(String appId);

  PageResponse<WorkflowFailureStrategy> listWorkflowFailureStrategies(PageRequest<WorkflowFailureStrategy> pageRequest);

  /**
   * Creates the.
   *
   * @param workflowFailureStrategy the workflow failure strategy
   * @return the workflow failure strategy
   */
  WorkflowFailureStrategy create(@Valid WorkflowFailureStrategy workflowFailureStrategy);

  WorkflowFailureStrategy update(@Valid WorkflowFailureStrategy workflowFailureStrategy);

  boolean deleteWorkflowFailureStrategy(String appId, String workflowFailureStrategyId);

  StateMachine readForEnv(String appId, String envId, String orchestrationId);
}
