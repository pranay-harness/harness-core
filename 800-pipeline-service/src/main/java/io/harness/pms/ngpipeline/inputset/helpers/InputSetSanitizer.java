package io.harness.pms.ngpipeline.inputset.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.InputSetYamlHelper;
import io.harness.pms.merger.helpers.TemplateHelper;

import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class InputSetSanitizer {
  public String sanitizeInputSet(String pipelineYaml, String inputSetYaml) {
    return sanitizeInputSet(pipelineYaml, inputSetYaml, true);
  }

  public String sanitizeRuntimeInput(String pipelineYaml, String runtimeInputYaml) {
    return sanitizeInputSet(pipelineYaml, runtimeInputYaml, false);
  }

  private String sanitizeInputSet(String pipelineYaml, String runtimeInput, boolean isInputSet) {
    String templateYaml = TemplateHelper.createTemplateFromPipeline(pipelineYaml);

    if (templateYaml == null) {
      return "";
    }

    // Strip off inputSet top key from yaml.
    // when its false, its runtimeInput (may be coming from trigger)
    if (isInputSet) {
      runtimeInput = InputSetYamlHelper.getPipelineComponent(runtimeInput);
    }

    String filteredInputSetYaml = TemplateHelper.removeRuntimeInputFromYaml(runtimeInput);
    if (EmptyPredicate.isEmpty(filteredInputSetYaml)) {
      return "";
    }
    PipelineYamlConfig inputSetConfig = new PipelineYamlConfig(filteredInputSetYaml);

    Set<FQN> invalidFQNsInInputSet =
        InputSetErrorsHelper.getInvalidFQNsInInputSet(templateYaml, filteredInputSetYaml).keySet();

    Map<FQN, Object> filtered = inputSetConfig.getFqnToValueMap()
                                    .entrySet()
                                    .stream()
                                    .filter(entry -> !invalidFQNsInInputSet.contains(entry.getKey()))
                                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new PipelineYamlConfig(filtered, inputSetConfig.getYamlMap()).getYaml();
  }
}
