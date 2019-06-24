package software.wings.beans.template;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.VariableType.ARTIFACT;

import com.google.inject.Inject;

import io.harness.beans.SweepingOutput;
import io.harness.serializer.KryoUtils;
import software.wings.beans.Variable;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateUtils {
  @Inject private SweepingOutputService sweepingOutputService;
  private void processArtifactVariable(ExecutionContext context, Variable variable) {
    String expression = getExpression(variable.getValue());
    Artifact artifact = (Artifact) context.evaluateExpression(expression);
    saveArtifactVariablesInSweepingOutput(context, variable.getName(), artifact);
  }

  private void saveArtifactVariablesInSweepingOutput(ExecutionContext context, String name, Artifact artifact) {
    if (artifact != null) {
      sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutput.Scope.STATE)
                                     .name(name)
                                     .output(KryoUtils.asDeflatedBytes(artifact))
                                     .build());
    }
  }

  private String getExpression(String value) {
    Pattern p = Pattern.compile("\\$\\{(.*?)\\}");
    Matcher m = p.matcher(value);
    if (m.find()) {
      return m.group(1);
    }
    return value;
  }

  public Map<String, Object> processTemplateVariables(ExecutionContext context, List<Variable> variables) {
    if (isEmpty(variables)) {
      return null;
    }
    Map<String, Object> map = new HashMap<>();
    for (Variable variable : variables) {
      if (variable.getName() != null && variable.getValue() != null) {
        if (!ARTIFACT.equals(variable.getType())) {
          map.put(variable.getName(), variable.getValue());
        } else {
          processArtifactVariable(context, variable);
        }
      }
    }
    return map;
  }
}
