package software.wings.yaml.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.FailureStrategyYaml;
import software.wings.beans.NotificationRuleYaml;
import software.wings.beans.TemplateExpressionYaml;
import software.wings.beans.VariableYaml;
import software.wings.beans.WorkflowPhaseYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 11/1/17
 */
@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CANARY")
public class CanaryWorkflowYaml extends WorkflowYaml {
  @Builder
  public CanaryWorkflowYaml(String type, String harnessApiVersion, String description,
      List<TemplateExpressionYaml> templateExpressions, String envName, boolean templatized,
      List<StepYaml> preDeploymentSteps, List<WorkflowPhaseYaml> phases, List<WorkflowPhaseYaml> rollbackPhases,
      List<StepYaml> postDeploymentSteps, List<NotificationRuleYaml> notificationRules,
      List<FailureStrategyYaml> failureStrategies, List<VariableYaml> userVariables, String concurrencyStrategy) {
    super(type, harnessApiVersion, description, templateExpressions, envName, templatized, preDeploymentSteps, phases,
        rollbackPhases, postDeploymentSteps, notificationRules, failureStrategies, userVariables, concurrencyStrategy,
        null, null, null, null);
  }
}
