package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.IncompleteStateException;
import software.wings.yaml.workflow.StepYaml;

import java.util.HashMap;
import java.util.List;

@TargetModule(HarnessModule._870_YAML_BEANS)
public class ApprovalStepCompletionYamlValidator implements StepCompletionYamlValidator {
  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    List<HashMap<String, Object>> templateExpressions =
        (List<HashMap<String, Object>>) changeContext.getYaml().getProperties().get("templateExpressions");
    if (!hasNone(templateExpressions)) {
      for (HashMap<String, Object> templateExpression : templateExpressions) {
        if (templateExpression.get("expression") == null) {
          throw new IncompleteStateException("Expression cannot be empty in templatized step");
        }
      }
    }
  }
}
