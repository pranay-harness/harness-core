package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.common.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by rishi on 12/21/16.
 */
@Entity(value = "orchWorkflows", noClassnameStored = true)
public class OrchestrationWorkflow extends Workflow {
  private WorkflowOrchestrationType workflowOrchestrationType;

  private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT);

  private List<String> workflowPhaseIds = new ArrayList<>();

  private Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();

  private Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = new HashMap<>();

  @Transient private List<WorkflowPhase> workflowPhases = new ArrayList<>();

  private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT, Constants.POST_DEPLOYMENT);

  private List<NotificationRule> notificationRules = new ArrayList<>();

  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  private List<Variable> systemVariables = new ArrayList<>();

  private List<Variable> userVariables = new ArrayList<>();

  private List<Variable> derivedVariables = new ArrayList<>();

  @Transient private List<WorkflowExecution> workflowExecutions = new ArrayList<>();

  private String envId;

  private Set<EntityType> requiredEntityTypes;

  public WorkflowOrchestrationType getWorkflowOrchestrationType() {
    return workflowOrchestrationType;
  }

  public void setWorkflowOrchestrationType(WorkflowOrchestrationType workflowOrchestrationType) {
    this.workflowOrchestrationType = workflowOrchestrationType;
  }

  public PhaseStep getPreDeploymentSteps() {
    return preDeploymentSteps;
  }

  public void setPreDeploymentSteps(PhaseStep preDeploymentSteps) {
    this.preDeploymentSteps = preDeploymentSteps;
  }

  public List<WorkflowPhase> getWorkflowPhases() {
    List<WorkflowPhase> workflowPhases = new ArrayList<>();
    for (String workflowPhaseId : workflowPhaseIds) {
      workflowPhases.add(workflowPhaseIdMap.get(workflowPhaseId));
    }
    return workflowPhases;
  }

  public void setWorkflowPhases(List<WorkflowPhase> workflowPhases) {
    this.workflowPhases = workflowPhases;
    if (workflowPhases != null) {
      workflowPhaseIds = new ArrayList<>();
      workflowPhaseIdMap = new HashMap<>();

      for (WorkflowPhase workflowPhase : workflowPhases) {
        workflowPhaseIds.add(workflowPhase.getUuid());
        workflowPhaseIdMap.put(workflowPhase.getUuid(), workflowPhase);
      }
    }
  }

  public PhaseStep getPostDeploymentSteps() {
    return postDeploymentSteps;
  }

  public void setPostDeploymentSteps(PhaseStep postDeploymentSteps) {
    this.postDeploymentSteps = postDeploymentSteps;
  }

  public List<NotificationRule> getNotificationRules() {
    return notificationRules;
  }

  public void setNotificationRules(List<NotificationRule> notificationRules) {
    this.notificationRules = notificationRules;
  }

  public List<FailureStrategy> getFailureStrategies() {
    return failureStrategies;
  }

  public void setFailureStrategies(List<FailureStrategy> failureStrategies) {
    this.failureStrategies = failureStrategies;
  }

  public List<Variable> getSystemVariables() {
    return systemVariables;
  }

  public void setSystemVariables(List<Variable> systemVariables) {
    this.systemVariables = systemVariables;
  }

  public List<Variable> getUserVariables() {
    return userVariables;
  }

  public void setUserVariables(List<Variable> userVariables) {
    this.userVariables = userVariables;
  }

  public List<Variable> getDerivedVariables() {
    return derivedVariables;
  }

  public void setDerivedVariables(List<Variable> derivedVariables) {
    this.derivedVariables = derivedVariables;
  }

  public List<WorkflowExecution> getWorkflowExecutions() {
    return workflowExecutions;
  }

  public void setWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
    this.workflowExecutions = workflowExecutions;
  }

  public List<String> getWorkflowPhaseIds() {
    return workflowPhaseIds;
  }

  public void setWorkflowPhaseIds(List<String> workflowPhaseIds) {
    this.workflowPhaseIds = workflowPhaseIds;
  }

  public Map<String, WorkflowPhase> getWorkflowPhaseIdMap() {
    return workflowPhaseIdMap;
  }

  public void setWorkflowPhaseIdMap(Map<String, WorkflowPhase> workflowPhaseIdMap) {
    this.workflowPhaseIdMap = workflowPhaseIdMap;
  }

  public Map<String, WorkflowPhase> getRollbackWorkflowPhaseIdMap() {
    return rollbackWorkflowPhaseIdMap;
  }

  public void setRollbackWorkflowPhaseIdMap(Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap) {
    this.rollbackWorkflowPhaseIdMap = rollbackWorkflowPhaseIdMap;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public Set<EntityType> getRequiredEntityTypes() {
    return requiredEntityTypes;
  }

  public void setRequiredEntityTypes(Set<EntityType> requiredEntityTypes) {
    this.requiredEntityTypes = requiredEntityTypes;
  }

  public static final class OrchestrationWorkflowBuilder {
    private WorkflowOrchestrationType workflowOrchestrationType;
    private String name;
    private PhaseStep preDeploymentSteps = new PhaseStep(PhaseStepType.PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT);
    private List<WorkflowPhase> workflowPhases = new ArrayList<>();
    private PhaseStep postDeploymentSteps = new PhaseStep(PhaseStepType.POST_DEPLOYMENT, Constants.POST_DEPLOYMENT);
    private List<NotificationRule> notificationRules = new ArrayList<>();
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private List<Variable> systemVariables = new ArrayList<>();
    private List<Variable> userVariables = new ArrayList<>();
    private List<Variable> derivedVariables = new ArrayList<>();
    private String uuid;
    private Graph graph;
    private String appId;
    private EmbeddedUser createdBy;
    private List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String envId;

    private OrchestrationWorkflowBuilder() {}

    public static OrchestrationWorkflowBuilder anOrchestrationWorkflow() {
      return new OrchestrationWorkflowBuilder();
    }

    public OrchestrationWorkflowBuilder withWorkflowOrchestrationType(
        WorkflowOrchestrationType workflowOrchestrationType) {
      this.workflowOrchestrationType = workflowOrchestrationType;
      return this;
    }

    public OrchestrationWorkflowBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public OrchestrationWorkflowBuilder withPreDeploymentSteps(PhaseStep preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public OrchestrationWorkflowBuilder addWorkflowPhases(WorkflowPhase workflowPhase) {
      this.workflowPhases.add(workflowPhase);
      return this;
    }

    public OrchestrationWorkflowBuilder withPostDeploymentSteps(PhaseStep postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public OrchestrationWorkflowBuilder withNotificationRules(List<NotificationRule> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public OrchestrationWorkflowBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public OrchestrationWorkflowBuilder withSystemVariables(List<Variable> systemVariables) {
      this.systemVariables = systemVariables;
      return this;
    }

    public OrchestrationWorkflowBuilder withUserVariables(List<Variable> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public OrchestrationWorkflowBuilder withDerivedVariables(List<Variable> derivedVariables) {
      this.derivedVariables = derivedVariables;
      return this;
    }

    public OrchestrationWorkflowBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public OrchestrationWorkflowBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public OrchestrationWorkflowBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public OrchestrationWorkflowBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public OrchestrationWorkflowBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public OrchestrationWorkflowBuilder withWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
      this.workflowExecutions = workflowExecutions;
      return this;
    }

    public OrchestrationWorkflowBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public OrchestrationWorkflowBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public OrchestrationWorkflowBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public OrchestrationWorkflow build() {
      OrchestrationWorkflow orchestrationWorkflow = new OrchestrationWorkflow();
      orchestrationWorkflow.setWorkflowOrchestrationType(workflowOrchestrationType);
      orchestrationWorkflow.setName(name);
      orchestrationWorkflow.setPreDeploymentSteps(preDeploymentSteps);
      orchestrationWorkflow.setWorkflowPhases(workflowPhases);
      orchestrationWorkflow.setPostDeploymentSteps(postDeploymentSteps);
      orchestrationWorkflow.setNotificationRules(notificationRules);
      orchestrationWorkflow.setFailureStrategies(failureStrategies);
      orchestrationWorkflow.setSystemVariables(systemVariables);
      orchestrationWorkflow.setUserVariables(userVariables);
      orchestrationWorkflow.setDerivedVariables(derivedVariables);
      orchestrationWorkflow.setUuid(uuid);
      orchestrationWorkflow.setGraph(graph);
      orchestrationWorkflow.setAppId(appId);
      orchestrationWorkflow.setEnvId(envId);
      orchestrationWorkflow.setCreatedBy(createdBy);
      orchestrationWorkflow.setWorkflowExecutions(workflowExecutions);
      orchestrationWorkflow.setCreatedAt(createdAt);
      orchestrationWorkflow.setLastUpdatedBy(lastUpdatedBy);
      orchestrationWorkflow.setLastUpdatedAt(lastUpdatedAt);
      return orchestrationWorkflow;
    }
  }
}
