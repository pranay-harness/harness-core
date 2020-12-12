package io.harness.ngtriggers.helpers;

import static io.harness.pms.merger.helpers.MergeHelper.mergeInputSetIntoPipeline;
import static io.harness.pms.merger.helpers.MergeHelper.sanitizeRuntimeInput;

import java.io.IOException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TriggerTargetExecutionHelper {
  public String sanitizeInputSet(String pipelineYaml, String inputSetYaml) throws IOException {
    return sanitizeRuntimeInput(pipelineYaml, inputSetYaml);
  }

  public String mergeInputSetIntoPipeline(String pipelineYaml, String inputSetYaml) throws IOException {
    return mergeInputSetIntoPipeline(pipelineYaml, inputSetYaml);
  }
}
