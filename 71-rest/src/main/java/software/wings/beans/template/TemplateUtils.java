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
  private static final Pattern p = Pattern.compile("\\$\\{(.*?)\\}");

  @Inject private SweepingOutputService sweepingOutputService;

  private void processArtifactVariable(ExecutionContext context, Variable variable) {
    String expression = getExpression(variable.getValue());
    Artifact artifact = (Artifact) context.evaluateExpression(expression);
    saveArtifactToSweepingOutput(context, variable.getName(), artifact);
  }

  private void saveArtifactToSweepingOutput(ExecutionContext context, String name, Artifact artifact) {
    if (artifact != null) {
      // this is to avoid saving the same artifact to sweeping output at state level in case of linked command template
      SweepingOutput sweepingOutputInput =
          context.prepareSweepingOutputBuilder(SweepingOutput.Scope.STATE).name(name).build();
      SweepingOutput result = sweepingOutputService.find(sweepingOutputInput.getAppId(), sweepingOutputInput.getName(),
          sweepingOutputInput.getPipelineExecutionId(), sweepingOutputInput.getWorkflowExecutionId(),
          sweepingOutputInput.getPhaseExecutionId(), sweepingOutputInput.getStateExecutionId());
      if (result == null) {
        sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutput.Scope.STATE)
                                       .name(name)
                                       .output(KryoUtils.asDeflatedBytes(artifact))
                                       .build());
      }
    }
  }

  public String getExpression(String value) {
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
