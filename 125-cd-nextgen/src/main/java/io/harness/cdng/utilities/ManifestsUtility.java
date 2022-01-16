/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.utilities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.YamlException;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class ManifestsUtility {
  public JsonNode getManifestsJsonNode() {
    String yamlField = "---\n"
        + "- manifest:\n"
        + "     identifier: manifestIdentifier\n"
        + "     spec:\n"
        + "     type: K8sManifest\n";

    YamlField manifestsYamlField;
    try {
      String yamlFieldWithUuid = YamlUtils.injectUuid(yamlField);
      manifestsYamlField = YamlUtils.readTree(yamlFieldWithUuid);
    } catch (IOException e) {
      throw new InvalidRequestException("Exception while creating stageOverrides");
    }
    return manifestsYamlField.getNode().getCurrJsonNode();
  }

  public YamlField fetchManifestsYamlFieldAndSetYamlUpdates(
      YamlField serviceField, Boolean isUseFromStage, YamlUpdates.Builder yamlUpdates) {
    if (isUseFromStage == false) {
      return serviceField.getNode()
          .getField(YamlTypes.SERVICE_DEFINITION)
          .getNode()
          .getField(YamlTypes.SPEC)
          .getNode()
          .getField(YamlTypes.MANIFEST_LIST_CONFIG);
    }
    YamlField stageOverrideField = serviceField.getNode().getField(YamlTypes.STAGE_OVERRIDES_CONFIG);

    if (stageOverrideField == null) {
      YamlField stageOverridesYamlField = fetchOverridesYamlField(serviceField);
      setYamlUpdate(stageOverridesYamlField, yamlUpdates);
      return stageOverridesYamlField.getNode().getField(YamlTypes.MANIFEST_LIST_CONFIG);
    }
    if (stageOverrideField.getNode().getField(YamlTypes.MANIFEST_LIST_CONFIG) == null) {
      YamlField artifactsYamlField = fetchManifestYamlFieldUnderStageOverride(stageOverrideField);
      setYamlUpdate(artifactsYamlField, yamlUpdates);
      return artifactsYamlField;
    }
    return stageOverrideField.getNode().getField(YamlTypes.MANIFEST_LIST_CONFIG);
  }

  private static YamlUpdates.Builder setYamlUpdate(YamlField yamlField, YamlUpdates.Builder yamlUpdates) {
    try {
      return yamlUpdates.putFqnToYaml(yamlField.getYamlPath(), YamlUtils.writeYamlString(yamlField));
    } catch (IOException e) {
      throw new YamlException(
          "Yaml created for yamlField at " + yamlField.getYamlPath() + " could not be converted into a yaml string");
    }
  }

  private YamlField fetchManifestYamlFieldUnderStageOverride(YamlField stageOverride) {
    return new YamlField(YamlTypes.MANIFEST_LIST_CONFIG,
        new YamlNode(YamlTypes.MANIFEST_LIST_CONFIG, ManifestsUtility.getManifestsJsonNode(), stageOverride.getNode()));
  }

  private YamlField fetchOverridesYamlField(YamlField serviceField) {
    return new YamlField(YamlTypes.STAGE_OVERRIDES_CONFIG,
        new YamlNode(YamlTypes.STAGE_OVERRIDES_CONFIG, StageOverridesUtility.getStageOverridesJsonNode(),
            serviceField.getNode()));
  }
}