package io.harness.cdng.utilities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.YamlException;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PrimaryArtifactsUtility {
  public JsonNode getArtifactsJsonNode() {
    String yamlField = "---\n"
        + "type: DockerRegistry\n"
        + "spec:\n";
    YamlField primaryYamlField;
    try {
      String yamlFieldWithUuid = YamlUtils.injectUuid(yamlField);
      primaryYamlField = YamlUtils.readTree(yamlFieldWithUuid);
    } catch (IOException e) {
      throw new InvalidRequestException("Exception while creating primary field");
    }
    return primaryYamlField.getNode().getCurrJsonNode();
  }

  public YamlField fetchPrimaryArtifactYamlFieldAndSetYamlUpdate(
      YamlField artifactField, YamlUpdates.Builder yamlUpdates) {
    YamlField primaryYamlField = artifactField.getNode().getField(YamlTypes.PRIMARY_ARTIFACT);

    if (primaryYamlField != null) {
      return primaryYamlField;
    }

    primaryYamlField = fetchPrimaryYamlFieldUnderArtifacts(artifactField);
    setYamlUpdate(primaryYamlField, yamlUpdates);
    return primaryYamlField;
  }

  private static YamlUpdates.Builder setYamlUpdate(YamlField yamlField, YamlUpdates.Builder yamlUpdates) {
    try {
      return yamlUpdates.putFqnToYaml(yamlField.getYamlPath(), YamlUtils.writeYamlString(yamlField));
    } catch (IOException e) {
      throw new YamlException(
          "Yaml created for yamlField at " + yamlField.getYamlPath() + " could not be converted into a yaml string");
    }
  }

  private YamlField fetchPrimaryYamlFieldUnderArtifacts(YamlField stageOverride) {
    return new YamlField(YamlTypes.PRIMARY_ARTIFACT,
        new YamlNode(
            YamlTypes.PRIMARY_ARTIFACT, PrimaryArtifactsUtility.getArtifactsJsonNode(), stageOverride.getNode()));
  }
}