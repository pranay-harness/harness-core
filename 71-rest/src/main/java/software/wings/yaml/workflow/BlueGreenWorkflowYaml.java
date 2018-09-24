package software.wings.yaml.workflow;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.FailureStrategy;
import software.wings.beans.NotificationRule;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.beans.Variable;
import software.wings.beans.WorkflowPhase;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("BLUE_GREEN")
@JsonPropertyOrder({"harnessApiVersion"})
@NoArgsConstructor
public class BlueGreenWorkflowYaml extends WorkflowYaml {
  @Builder
  public BlueGreenWorkflowYaml(String type, String harnessApiVersion, String description,
      List<Yaml> templateExpressions, String envName, boolean templatized, List<StepYaml> preDeploymentSteps,
      List<WorkflowPhase.Yaml> phases, List<WorkflowPhase.Yaml> rollbackPhases, List<StepYaml> postDeploymentSteps,
      List<NotificationRule.Yaml> notificationRules, List<FailureStrategy.Yaml> failureStrategies,
      List<Variable.Yaml> userVariables) {
    super(type, harnessApiVersion, description, templateExpressions, envName, templatized, preDeploymentSteps, phases,
        rollbackPhases, postDeploymentSteps, notificationRules, failureStrategies, userVariables);
  }
}