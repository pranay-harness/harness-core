package software.wings.service.intfc;

import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.InstanceElement;
import software.wings.beans.EntityType;
import software.wings.beans.FailureStrategy;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
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
import software.wings.service.intfc.manipulation.SettingsServiceManipulationObserver;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.Stencil;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * The Interface WorkflowService.
 *
 * @author Rishi
 */
public interface WorkflowService extends OwnedByApplication, SettingsServiceManipulationObserver {
  PageResponse<Workflow> listWorkflows(PageRequest<Workflow> pageRequest);
  List<String> isEnvironmentReferenced(String appId, @NotEmpty String envId);

  PageResponse<Workflow> listWorkflowsWithoutOrchestration(PageRequest<Workflow> pageRequest);

  PageResponse<Workflow> listWorkflows(PageRequest<Workflow> pageRequest, Integer previousExecutionsCount);

  Workflow readWorkflow(@NotNull String appId, @NotNull String workflowId, Integer version);

  Workflow readWorkflow(@NotNull String appId, @NotNull String workflowId);

  Workflow readWorkflowWithoutOrchestration(@NotNull String appId, @NotNull String workflowId);

  Workflow readWorkflowByName(String appId, String workflowName);

  @ValidationGroups(Create.class) Workflow createWorkflow(@Valid Workflow workflow);

  boolean ensureArtifactCheck(String appId, OrchestrationWorkflow orchestrationWorkflow);

  @ValidationGroups(Update.class) Workflow updateWorkflow(@Valid Workflow workflow);

  @ValidationGroups(Update.class) Workflow updateLinkedWorkflow(@Valid Workflow workflow, Workflow existingWorkflow);

  @ValidationGroups(Update.class)
  Workflow updateWorkflow(@Valid Workflow workflow, OrchestrationWorkflow orchestrationWorkflow);

  @ValidationGroups(Update.class)
  Workflow updateWorkflow(@Valid Workflow workflow, OrchestrationWorkflow orchestrationWorkflow,
      boolean inframappingChanged, boolean envChanged, boolean cloned);

  boolean deleteWorkflow(String appId, String workflowId);

  StateMachine createStateMachine(@Valid StateMachine stateMachine);

  StateMachine readLatestStateMachine(String appId, String originId);

  PageResponse<StateMachine> listStateMachines(PageRequest<StateMachine> req);

  Map<StateTypeScope, List<Stencil>> stencils(
      String appId, String workflowId, String phaseId, StateTypeScope... stateTypeScopes);

  StateMachine readStateMachine(String appId, String originId, Integer version);

  Workflow readLatestSimpleWorkflow(String appId, String envId);

  Map<String, StateTypeDescriptor> stencilMap();

  PhaseStep updatePreDeployment(String appId, String workflowId, PhaseStep phaseStep);

  PhaseStep updatePostDeployment(String appId, String workflowId, PhaseStep phaseStep);

  WorkflowPhase createWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase);

  @ValidationGroups(Update.class)
  WorkflowPhase updateWorkflowPhase(
      @NotEmpty String appId, @NotEmpty String workflowId, @Valid WorkflowPhase workflowPhase);

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

  void pruneDescendingEntities(@org.hibernate.validator.constraints.NotEmpty String appId,
      @org.hibernate.validator.constraints.NotEmpty String workflowId);

  boolean workflowHasSshInfraMapping(String appId, String workflowId);

  String getHPAYamlStringWithCustomMetric(
      Integer minAutoscaleInstances, Integer maxAutoscaleInstances, Integer targetCpuUtilizationPercentage);

  void loadOrchestrationWorkflow(Workflow workflow, Integer version);

  List<Service> getResolvedServices(Workflow workflow, Map<String, String> workflowVariables);

  List<InfrastructureMapping> getResolvedInfraMappings(Workflow workflow, Map<String, String> workflowVariables);

  /**
   * Resolves the inframappingIds from workflow variables
   * @param workflow
   * @param workflowVariables
   * @return
   */
  List<String> getResolvedInfraMappingIds(Workflow workflow, Map<String, String> workflowVariables);

  List<InstanceElement> getDeployedNodes(String appId, String workflowId);

  String resolveEnvironmentId(Workflow workflow, Map<String, String> workflowVariables);

  GraphNode readGraphNode(@NotEmpty String appId, @NotEmpty String workflowId, @NotEmpty String nodeId);

  List<EntityType> getRequiredEntities(String appId, String workflowId);

  Set<EntityType> fetchRequiredEntityTypes(String appId, OrchestrationWorkflow orchestrationWorkflow);

  boolean deleteByYamlGit(String appId, String workflowId, boolean syncFromGit);
}
