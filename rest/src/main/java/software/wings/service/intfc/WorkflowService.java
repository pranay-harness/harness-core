package software.wings.service.intfc;

import software.wings.beans.FailureStrategy;
import software.wings.beans.GraphNode;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.stats.CloneMetadata;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
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
public interface WorkflowService extends OwnedByApplication, OwnedByEnvironment {
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
   * @param appId      the app id
   * @param workflowId the workflow id
   * @param version    the version
   * @return the workflow
   */
  Workflow readWorkflow(@NotNull String appId, @NotNull String workflowId, Integer version);

  /**
   * Read Workflow.
   *
   * @param appId      the app id
   * @param workflowId the workflow id
   * @return the workflow
   */
  Workflow readWorkflow(@NotNull String appId, @NotNull String workflowId);

  Workflow readWorkflowByName(String appId, String workflowName);

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
   * Update workflow.
   *
   * @param workflow            the workflow
   * @param inframappingChanged Inframapping changed or not
   * @param envChanged          Env changed or not
   * @param cloned              cloned request or not
   * @return the workflow
   */
  Workflow updateWorkflow(@Valid Workflow workflow, OrchestrationWorkflow orchestrationWorkflow,
      boolean inframappingChanged, boolean envChanged, boolean cloned);

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

  StateMachine readStateMachine(String appId, String stateMachineId);

  /**
   * Read latest simple workflow .
   *
   * @param appId the app id
   * @param envId the environment id
   * @return the workflow
   */
  Workflow readLatestSimpleWorkflow(String appId, String envId);

  /**
   * Stencil map map.
   *
   * @return the map
   */
  Map<String, StateTypeDescriptor> stencilMap();

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
      String appId, String workflowId, @Valid List<FailureStrategy> failureStrategies);

  List<Variable> updateUserVariables(String appId, String workflowId, List<Variable> userVariables);

  GraphNode updateGraphNode(
      @NotNull String appId, @NotNull String workflowId, @NotNull String subworkflowId, GraphNode node);

  Workflow cloneWorkflow(String appId, String workflowId, Workflow workflow);

  Workflow updateWorkflow(String appId, String workflowId, Integer defaultVersion);

  Workflow cloneWorkflow(String appId, String workflowId, CloneMetadata cloneMetadata);

  WorkflowPhase cloneWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase);

  Map<String, String> getStateDefaults(@NotNull String appId, @NotNull String serviceId, @NotNull StateType stateType);

  List<Service> resolveServices(Workflow workflow, Map<String, String> workflowVariables);

  /**
   * Prune workflow descending objects.
   *
   * @param appId      the app id
   * @param workflowId the workflow id
   */
  void pruneDescendingEntities(@org.hibernate.validator.constraints.NotEmpty String appId,
      @org.hibernate.validator.constraints.NotEmpty String workflowId);

  boolean workflowHasSshInfraMapping(String appId, String workflowId);

  String getHPAYamlStringWithCustomMetric(
      Integer minAutoscaleInstances, Integer maxAutoscaleInstances, Integer targetCpuUtilizationPercentage);
}
