package software.wings.service.intfc;

import software.wings.beans.Base;
import software.wings.beans.Orchestration;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowFailureStrategy;
import software.wings.beans.WorkflowOuterSteps;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeDescriptor;
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
   * @param version  the version
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
  <T extends Base> boolean deleteWorkflow(Class<T> cls, String appId, String workflowId);

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
   * @param envId       the env id
   * @return the page response
   */
  PageResponse<Orchestration> listOrchestration(PageRequest<Orchestration> pageRequest, String envId);

  /**
   * Read orchestration.
   *
   * @param appId           the app id
   * @param orchestrationId the orchestration id
   * @param version         the version
   * @return the orchestration
   */
  Orchestration readOrchestration(@NotNull String appId, @NotNull String orchestrationId, Integer version);

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

  /**
   * List workflow failure strategies list.
   *
   * @param appId the app id
   * @return the list
   */
  List<WorkflowFailureStrategy> listWorkflowFailureStrategies(String appId);

  /**
   * List workflow failure strategies page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<WorkflowFailureStrategy> listWorkflowFailureStrategies(PageRequest<WorkflowFailureStrategy> pageRequest);

  /**
   * Creates the.
   *
   * @param workflowFailureStrategy the workflow failure strategy
   * @return the workflow failure strategy
   */
  WorkflowFailureStrategy create(@Valid WorkflowFailureStrategy workflowFailureStrategy);

  /**
   * Update workflow failure strategy.
   *
   * @param workflowFailureStrategy the workflow failure strategy
   * @return the workflow failure strategy
   */
  WorkflowFailureStrategy update(@Valid WorkflowFailureStrategy workflowFailureStrategy);

  /**
   * Delete workflow failure strategy boolean.
   *
   * @param appId                     the app id
   * @param workflowFailureStrategyId the workflow failure strategy id
   * @return the boolean
   */
  boolean deleteWorkflowFailureStrategy(String appId, String workflowFailureStrategyId);

  /**
   * Read for env state machine.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param orchestrationId the orchestration id
   * @return the state machine
   */
  StateMachine readForEnv(String appId, String envId, String orchestrationId);

  /**
   * Stencil map map.
   *
   * @return the map
   */
  public Map<String, StateTypeDescriptor> stencilMap();

  PageResponse<OrchestrationWorkflow> listOrchestrationWorkflows(PageRequest<OrchestrationWorkflow> pageRequest);

  OrchestrationWorkflow readOrchestrationWorkflow(String appId, String orchestrationWorkflowId);

  OrchestrationWorkflow createOrchestrationWorkflow(OrchestrationWorkflow orchestrationWorkflow);

  boolean deleteOrchestrationWorkflow(String appId, String orchestrationWorkflowId);

  WorkflowOuterSteps updatePreDeployment(
      String appId, String orchestrationWorkflowId, WorkflowOuterSteps workflowOuterSteps);

  WorkflowOuterSteps updatePostDeployment(
      String appId, String orchestrationWorkflowId, WorkflowOuterSteps workflowOuterSteps);

  WorkflowPhase createWorkflowPhase(String appId, String orchestrationWorkflowId, WorkflowPhase workflowPhase);

  WorkflowPhase updateWorkflowPhase(String appId, String orchestrationWorkflowId, WorkflowPhase workflowPhase);

  void deleteWorkflowPhase(String appId, String orchestrationWorkflowId, String phaseId);
}
