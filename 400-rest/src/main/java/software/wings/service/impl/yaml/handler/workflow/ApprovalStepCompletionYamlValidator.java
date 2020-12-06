package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.IncompleteStateException;
import software.wings.yaml.workflow.StepYaml;

import java.util.HashMap;
import java.util.List;

public class ApprovalStepCompletionYamlValidator implements StepCompletionYamlValidator {
  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    List<HashMap<String, Object>> templateExpressions =
        (List<HashMap<String, Object>>) changeContext.getYaml().getProperties().get("templateExpressions");
    if (!isEmpty(templateExpressions)) {
      for (HashMap<String, Object> templateExpression : templateExpressions) {
        if (templateExpression.get("expression") == null) {
          throw new IncompleteStateException("Expression cannot be empty in templatized step");
        }
      }
    }
  }
}
