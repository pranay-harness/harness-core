package software.wings.service.impl.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static java.lang.String.format;
import static software.wings.sm.StateType.APPROVAL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.sm.states.ApprovalState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
public class PipelineServiceValidator {
  private PipelineServiceValidator() {}
  public static boolean validateTemplateExpressions(Pipeline pipeline) {
    List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    if (pipelineStages != null) {
      for (PipelineStage pipelineStage : pipelineStages) {
        for (PipelineStageElement pse : pipelineStage.getPipelineStageElements()) {
          if (APPROVAL.name().equals(pse.getType())) {
            Map<String, Object> properties = pse.getProperties();
            List<Map<String, Object>> templateExpressions =
                (List<Map<String, Object>>) properties.get("templateExpressions");
            if (!isEmpty(templateExpressions)) {
              for (Map<String, Object> templateExpression : templateExpressions) {
                if (templateExpression != null) {
                  String expression = (String) templateExpression.get("expression");
                  if (!matchesVariablePattern(expression)) {
                    throw new InvalidRequestException("Template variable:[" + expression
                            + "] not in proper format ,should start with ${ and end with }, only a-zA-Z0-9_- allowed",
                        USER);
                  }
                }
              }
            }
          }
        }
      }
    }
    return true;
  }

  public static void checkUniqueApprovalPublishedVariable(Pipeline pipeline) {
    if (isEmpty(pipeline.getPipelineStages())) {
      return;
    }
    Map<String, String> publishedVarToStage = new HashMap<>();
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      PipelineStageElement stageElement = pipelineStage.getPipelineStageElements().get(0);
      if (APPROVAL.name().equals(stageElement.getType())) {
        String sweepingOutputName = ApprovalState.fetchAndTrimSweepingOutputName(stageElement.getProperties());
        if (isNotEmpty(sweepingOutputName)) {
          if (publishedVarToStage.containsKey(sweepingOutputName)) {
            throw new InvalidRequestException(
                format(
                    "You cannot use the same Publish Variable Name [%s] in multiple Approval stages. Publish Variable Name [%s] already used in stage [%s]",
                    sweepingOutputName, sweepingOutputName, publishedVarToStage.get(sweepingOutputName)),
                prepareExplanation(), USER);
          }
          publishedVarToStage.put(sweepingOutputName, pipelineStage.getName());
        }
      }
    }
  }

  @NotNull
  private static ExplanationException prepareExplanation() {
    return new ExplanationException("Each Publish Variable Name is used to distinguish one stage’s variables.",
        new HintException("Give each Approval stage a unique Publish Variable Name"));
  }
}
