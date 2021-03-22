package software.wings.yaml.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.FailureStrategyYaml;
import software.wings.beans.NotificationRuleYaml;
import software.wings.beans.TemplateExpression;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.beans.VariableYaml;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.workflow.StepSkipStrategy;
import software.wings.yaml.BaseEntityYaml;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Base workflow yaml
 * @author rktummala on 10/26/17
 */
@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
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
  private List<NotificationRuleYaml> notificationRules = new ArrayList<>();
  private List<FailureStrategyYaml> failureStrategies = new ArrayList<>();
  private List<VariableYaml> userVariables = new ArrayList<>();
  private String concurrencyStrategy;

  private List<FailureStrategyYaml> preDeploymentFailureStrategy = new ArrayList<>();
  private List<FailureStrategyYaml> postDeploymentFailureStrategy = new ArrayList<>();

  private List<StepSkipStrategy.Yaml> preDeploymentStepSkipStrategy = new ArrayList<>();
  private List<StepSkipStrategy.Yaml> postDeploymentStepSkipStrategy = new ArrayList<>();

  public WorkflowYaml(String type, String harnessApiVersion, String description, List<Yaml> templateExpressions,
      String envName, boolean templatized, List<StepYaml> preDeploymentSteps, List<WorkflowPhase.Yaml> phases,
      List<WorkflowPhase.Yaml> rollbackPhases, List<StepYaml> postDeploymentSteps,
      List<NotificationRuleYaml> notificationRules, List<FailureStrategyYaml> failureStrategies,
      List<VariableYaml> userVariables, String concurrencyStrategy,
      List<FailureStrategyYaml> preDeploymentFailureStrategy, List<FailureStrategyYaml> postDeploymentFailureStrategy,
      List<StepSkipStrategy.Yaml> preDeploymentStepSkipStrategy,
      List<StepSkipStrategy.Yaml> postDeploymentStepSkipStrategy) {
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
    this.concurrencyStrategy = concurrencyStrategy;
    this.preDeploymentFailureStrategy = preDeploymentFailureStrategy;
    this.postDeploymentFailureStrategy = postDeploymentFailureStrategy;
    this.preDeploymentStepSkipStrategy = preDeploymentStepSkipStrategy;
    this.postDeploymentStepSkipStrategy = postDeploymentStepSkipStrategy;
  }
}
