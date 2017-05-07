package software.wings.service.intfc;

import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph.Node;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
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
   * List Workflow.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Workflow> listWorkflows(PageRequest<Workflow> pageRequest);

  PageResponse<Workflow> listWorkflows(PageRequest<Workflow> pageRequest, Integer previousExecutionsCount);

  /**
   * Read Workflow.
   *
   * @param appId           the app id
   * @param workflowId the workflow id
   * @param version         the version
   * @return the workflow
   */
  Workflow readWorkflow(@NotNull String appId, @NotNull String workflowId, Integer version);

  /**
   * Read Workflow.
   *
   * @param appId           the app id
   * @param workflowId the workflow id
   * @return the workflow
   */
  Workflow readWorkflow(@NotNull String appId, @NotNull String workflowId);

  /**
   * Creates the workflow.
   *
   * @param workflow the workflow
   * @return the workflow
   */
  Workflow createWorkflow(@Valid Workflow workflow);

  /**
   * Update workflow.
   *
   * @param workflow the workflow
   * @return the workflow
   */
  Workflow updateWorkflow(@Valid Workflow workflow);

  /**
   * Update workflow.
   *
   * @param workflow the workflow
   * @return the workflow
   */
  Workflow updateWorkflow(@Valid Workflow workflow, OrchestrationWorkflow orchestrationWorkflow);

  /**
   * Delete workflow.
   *
   * @param appId      the app id
   * @param workflowId the workflow id
   * @return the boolean
   */
  boolean deleteWorkflow(String appId, String workflowId);

  /**
   * Creates the.
   *
   * @param stateMachine the state machine
   * @return the state machine
   */
  StateMachine createStateMachine(@Valid StateMachine stateMachine);

  /**
   * Read latest.
   *
   * @param appId    the app id
   * @param originId the origin id
   * @return the state machine
   */
  StateMachine readLatestStateMachine(String appId, String originId);

  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<StateMachine> listStateMachines(PageRequest<StateMachine> req);

  /**
   * Stencils.
   *
   * @param appId           the app id
   * @param stateTypeScopes the state type scopes
   * @return the map
   */
  Map<StateTypeScope, List<Stencil>> stencils(
      String appId, String workflowId, String phaseId, StateTypeScope... stateTypeScopes);

  StateMachine readStateMachine(String appId, String originId, Integer version);

  /**
   * Read latest simple workflow .
   *
   * @param appId the app id
   * @param envId the environment id
   * @return the workflow
   */
  Workflow readLatestSimpleWorkflow(String appId, String envId);

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
   * Stencil map map.
   *
   * @return the map
   */
  public Map<String, StateTypeDescriptor> stencilMap();

  PhaseStep updatePreDeployment(String appId, String workflowId, PhaseStep phaseStep);

  PhaseStep updatePostDeployment(String appId, String workflowId, PhaseStep phaseStep);

  WorkflowPhase createWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase);

  WorkflowPhase updateWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase);

  WorkflowPhase updateWorkflowPhaseRollback(
      String appId, String workflowId, String phaseId, WorkflowPhase workflowPhase);

  void deleteWorkflowPhase(String appId, String workflowId, String phaseId);

  List<NotificationRule> updateNotificationRules(
      String appId, String workflowId, List<NotificationRule> notificationRules);

  List<FailureStrategy> updateFailureStrategies(
      String appId, String workflowId, List<FailureStrategy> failureStrategies);

  List<Variable> updateUserVariables(String appId, String workflowId, List<Variable> userVariables);

  Node updateGraphNode(@NotNull String appId, @NotNull String workflowId, @NotNull String subworkflowId, Node node);
}
