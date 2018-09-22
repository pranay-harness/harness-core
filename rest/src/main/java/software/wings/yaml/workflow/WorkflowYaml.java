package software.wings.yaml.workflow;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
@JsonSubTypes({
  @Type(value = BasicWorkflowYaml.class, name = "BASIC")
  , @Type(value = RollingWorkflowYaml.class, name = "ROLLING"),
      @Type(value = BlueGreenWorkflowYaml.class, name = "BLUE_GREEN"),
      @Type(value = CanaryWorkflowYaml.class, name = "CANARY"), @Type(value = BuildWorkflowYaml.class, name = "BUILD"),
      @Type(value = MultiServiceWorkflowYaml.class, name = "MULTI_SERVICE")
})
public abstract class WorkflowYaml extends BaseEntityYaml {
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

  public WorkflowYaml(String type, String harnessApiVersion, String description, List<Yaml> templateExpressions,
      String envName, boolean templatized, List<StepYaml> preDeploymentSteps, List<WorkflowPhase.Yaml> phases,
      List<WorkflowPhase.Yaml> rollbackPhases, List<StepYaml> postDeploymentSteps,
      List<NotificationRule.Yaml> notificationRules, List<FailureStrategy.Yaml> failureStrategies,
      List<Variable.Yaml> userVariables) {
    super(type, harnessApiVersion);
    this.description = description;
    this.templateExpressions = templateExpressions;
    this.envName = envName;
    this.templatized = templatized;
    this.preDeploymentSteps = preDeploymentSteps;
    this.phases = phases;
    this.rollbackPhases = rollbackPhases;
    this.postDeploymentSteps = postDeploymentSteps;
    this.notificationRules = notificationRules;
    this.failureStrategies = failureStrategies;
    this.userVariables = userVariables;
  }
}
