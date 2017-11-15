package software.wings.yaml.workflow;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.FailureStrategy;
import software.wings.beans.NotificationRule;
import software.wings.beans.TemplateExpression;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.beans.Variable;
import software.wings.beans.WorkflowPhase;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;

/**
 * Base workflow yaml
 * @author rktummala on 10/26/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowYaml extends BaseEntityYaml {
  private String name;
  private String description;
  private List<TemplateExpression.Yaml> templateExpressions;
  private String envName;
  private boolean templatized;

  private List<StepYaml> preDeploymentSteps = new ArrayList<>();
  private List<WorkflowPhase.Yaml> phases = new ArrayList<>();
  private List<WorkflowPhase.Yaml> rollbackPhases = new ArrayList<>();
  private List<StepYaml> postDeploymentSteps = new ArrayList<>();
  private List<NotificationRule.Yaml> notificationRules = new ArrayList<>();
  private List<FailureStrategy.Yaml> failureStrategies = new ArrayList<>();
  private List<Variable.Yaml> userVariables = new ArrayList<>();

  public static abstract class Builder {
    private String type;
    private String name;
    private String description;
    private List<Yaml> templateExpressions;
    private String envName;
    private boolean templatized;
    private List<StepYaml> preDeploymentSteps = new ArrayList<>();
    private List<WorkflowPhase.Yaml> phases = new ArrayList<>();
    private List<WorkflowPhase.Yaml> rollbackPhases = new ArrayList<>();
    private List<StepYaml> postDeploymentSteps = new ArrayList<>();
    private List<NotificationRule.Yaml> notificationRules = new ArrayList<>();
    private List<FailureStrategy.Yaml> failureStrategies = new ArrayList<>();
    private List<Variable.Yaml> userVariables = new ArrayList<>();

    protected Builder() {}

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withTemplateExpressions(List<Yaml> templateExpressions) {
      this.templateExpressions = templateExpressions;
      return this;
    }

    public Builder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public Builder withTemplatized(boolean templatized) {
      this.templatized = templatized;
      return this;
    }

    public Builder withPreDeploymentSteps(List<StepYaml> preDeploymentSteps) {
      this.preDeploymentSteps = preDeploymentSteps;
      return this;
    }

    public Builder withPhases(List<WorkflowPhase.Yaml> phases) {
      this.phases = phases;
      return this;
    }

    public Builder withRollbackPhases(List<WorkflowPhase.Yaml> rollbackPhases) {
      this.rollbackPhases = rollbackPhases;
      return this;
    }

    public Builder withPostDeploymentSteps(List<StepYaml> postDeploymentSteps) {
      this.postDeploymentSteps = postDeploymentSteps;
      return this;
    }

    public Builder withNotificationRules(List<NotificationRule.Yaml> notificationRules) {
      this.notificationRules = notificationRules;
      return this;
    }

    public Builder withFailureStrategies(List<FailureStrategy.Yaml> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public Builder withUserVariables(List<Variable.Yaml> userVariables) {
      this.userVariables = userVariables;
      return this;
    }

    public <T extends WorkflowYaml> T build() {
      T workflowYaml = getWorkflowYaml();
      workflowYaml.setType(type);
      workflowYaml.setName(name);
      workflowYaml.setDescription(description);
      workflowYaml.setTemplateExpressions(templateExpressions);
      workflowYaml.setEnvName(envName);
      workflowYaml.setTemplatized(templatized);
      workflowYaml.setPreDeploymentSteps(preDeploymentSteps);
      workflowYaml.setPhases(phases);
      workflowYaml.setRollbackPhases(rollbackPhases);
      workflowYaml.setPostDeploymentSteps(postDeploymentSteps);
      workflowYaml.setNotificationRules(notificationRules);
      workflowYaml.setFailureStrategies(failureStrategies);
      workflowYaml.setUserVariables(userVariables);
      return workflowYaml;
    }

    protected abstract <T extends WorkflowYaml> T getWorkflowYaml();
  }
}
