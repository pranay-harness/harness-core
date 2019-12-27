package software.wings.service.impl.pipeline;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.sm.StateType.APPROVAL;

import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.sm.states.ApprovalState;

import java.util.HashMap;
import java.util.Map;

public class PipelineServiceValidator {
  private PipelineServiceValidator() {}
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
