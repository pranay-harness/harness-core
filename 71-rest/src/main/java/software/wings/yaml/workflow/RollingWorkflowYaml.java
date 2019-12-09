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

/**
 * @author rktummala on 11/1/17
 */
@Data
@JsonTypeName("ROLLING")
@JsonPropertyOrder({"harnessApiVersion"})
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RollingWorkflowYaml extends WorkflowYaml {
  @Builder
  public RollingWorkflowYaml(String type, String harnessApiVersion, String description, List<Yaml> templateExpressions,
      String envName, boolean templatized, List<StepYaml> preDeploymentSteps, List<WorkflowPhase.Yaml> phases,
      List<WorkflowPhase.Yaml> rollbackPhases, List<StepYaml> postDeploymentSteps,
      List<NotificationRule.Yaml> notificationRules, List<FailureStrategy.Yaml> failureStrategies,
      List<Variable.Yaml> userVariables, String concurrencyStrategy) {
    super(type, harnessApiVersion, description, templateExpressions, envName, templatized, preDeploymentSteps, phases,
        rollbackPhases, postDeploymentSteps, notificationRules, failureStrategies, userVariables, concurrencyStrategy,
        null, null);
  }
}