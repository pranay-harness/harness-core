package io.harness.cdng.utilities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class StageOverridesUtility {
  public JsonNode getStageOverridesJsonNode() {
    String yamlField = "---\n"
        + "artifacts:\n"
        + "  primary:\n"
        + "  sidecars: []\n"
        + "manifest: []\n"
        + "variables: []\n";
    YamlField overrideYamlField;
    try {
      String yamlFieldWithUuid = YamlUtils.injectUuid(yamlField);
      overrideYamlField = YamlUtils.readTree(yamlFieldWithUuid);
    } catch (IOException e) {
      throw new InvalidRequestException("Exception while creating stageOverrides");
    }
    return overrideYamlField.getNode().getCurrJsonNode();
  }
}