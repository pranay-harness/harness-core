package software.wings.beans;

import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Graph.Builder;
import software.wings.common.UUIDGenerator;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rishi on 12/21/16.
 */
@Entity(value = "orchWorkflows", noClassnameStored = true)
public class OrchestrationWorkflow extends Base {
  private static final String PRE_DEPLOYMENT_STEPS = "Pre-deployment Steps";
  private static final String POST_DEPLOYMENT_STEPS = "Post-deployment Steps";

  private WorkflowOrchestrationType workflowOrchestrationType;

  @NotNull private String name;

  private WorkflowOuterSteps preDeploymentSteps = new WorkflowOuterSteps();
  ;

  private List<WorkflowPhase> workflowPhases = new ArrayList<>();

  private WorkflowOuterSteps postDeploymentSteps = new WorkflowOuterSteps();
  ;

  private List<NotificationRule> notificationRules = new ArrayList<>();

  private List<FailureStrategy> failureStrategies = new ArrayList<>();
  ;

  private List<Variable> systemVariables = new ArrayList<>();

  private List<Variable> userVariables = new ArrayList<>();

  private List<Variable> derivedVariables = new ArrayList<>();

  @Transient private List<WorkflowExecution> workflowExecutions = new ArrayList<>();

  public WorkflowOrchestrationType getWorkflowOrchestrationType() {
    return workflowOrchestrationType;
  }

  public void setWorkflowOrchestrationType(WorkflowOrchestrationType workflowOrchestrationType) {
    this.workflowOrchestrationType = workflowOrchestrationType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WorkflowOuterSteps getPreDeploymentSteps() {
    return preDeploymentSteps;
  }

  public void setPreDeploymentSteps(WorkflowOuterSteps preDeploymentSteps) {
    this.preDeploymentSteps = preDeploymentSteps;
  }

  public List<WorkflowPhase> getWorkflowPhases() {
    return workflowPhases;
  }

  public void setWorkflowPhases(List<WorkflowPhase> workflowPhases) {
    this.workflowPhases = workflowPhases;
  }

  public WorkflowOuterSteps getPostDeploymentSteps() {
    return postDeploymentSteps;
  }

  public void setPostDeploymentSteps(WorkflowOuterSteps postDeploymentSteps) {
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

  public Graph getGraph() {
    String id1 = UUIDGenerator.getUuid();
    String id2;
    Builder graphBuilder =
        aGraph().addNodes(aNode().withId(id1).withName(PRE_DEPLOYMENT_STEPS).withType(StateType.GROUP.name()).build());

    if (workflowPhases != null) {
      for (WorkflowPhase workflowPhase : workflowPhases) {
        id2 = UUIDGenerator.getUuid();
        graphBuilder.addNodes(
            aNode().withId(id2).withName(workflowPhase.getName()).withType(StateType.GROUP.name()).build());
        graphBuilder.addLinks(aLink()
                                  .withId(UUIDGenerator.getUuid())
                                  .withFrom(id1)
                                  .withTo(id2)
                                  .withType(ExecutionStatus.SUCCESS.name())
                                  .build());
        id1 = id2;
      }
    }
    id2 = UUIDGenerator.getUuid();
    graphBuilder.addNodes(aNode().withId(id2).withName(PRE_DEPLOYMENT_STEPS).withType(StateType.GROUP.name()).build());
    graphBuilder.addLinks(aLink()
                              .withId(UUIDGenerator.getUuid())
                              .withFrom(id1)
                              .withTo(id2)
                              .withType(ExecutionStatus.SUCCESS.name())
                              .build());

    return graphBuilder.build();
  }

  public void setGraph(Graph graph) {}

  public static final class OrchestrationWorkflowBuilder {
    private WorkflowOrchestrationType workflowOrchestrationType;
    private String name;
    private WorkflowOuterSteps preDeploymentSteps = new WorkflowOuterSteps();
    private List<WorkflowPhase> workflowPhases = new ArrayList<>();
    private WorkflowOuterSteps postDeploymentSteps = new WorkflowOuterSteps();
    ;
    private List<NotificationRule> notificationRules = new ArrayList<>();
    ;
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    ;
    private List<Variable> systemVariables = new ArrayList<>();
    ;
    private List<Variable> userVariables = new ArrayList<>();
    ;
    private List<Variable> derivedVariables = new ArrayList<>();
    ;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

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

    public OrchestrationWorkflowBuilder withPreDeploymentSteps(WorkflowOuterSteps preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public OrchestrationWorkflowBuilder withWorkflowPhases(List<WorkflowPhase> workflowPhases) {
      this.workflowPhases = workflowPhases;
      return this;
    }

    public OrchestrationWorkflowBuilder withPostDeploymentSteps(WorkflowOuterSteps postDeploymentSteps) {
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

    public OrchestrationWorkflowBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public OrchestrationWorkflowBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
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
      orchestrationWorkflow.setAppId(appId);
      orchestrationWorkflow.setCreatedBy(createdBy);
      orchestrationWorkflow.setCreatedAt(createdAt);
      orchestrationWorkflow.setLastUpdatedBy(lastUpdatedBy);
      orchestrationWorkflow.setLastUpdatedAt(lastUpdatedAt);
      return orchestrationWorkflow;
    }
  }
}
