/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.YamlException;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class SideCarsListArtifactsUtility {
    public JsonNode getSideCarsListJsonNode() {
        String yamlField = "---\n";
        YamlField sideCarsYamlField;
        try {
            String yamlFieldWithUuid = YamlUtils.injectUuid(yamlField);
            sideCarsYamlField = YamlUtils.readTree(yamlFieldWithUuid);
        } catch (IOException e) {
            throw new InvalidRequestException("Exception while creating primary field");
        }
        return sideCarsYamlField.getNode().getCurrJsonNode();
    }

    public JsonNode getIndividualSideCarsListJsonNode(String identifier) {
        String yamlField = "---\n"
                + "- sidecar:\n"
                + "      type: DockerRegistry\n"
                + "      identifier: \"" + identifier + "\"\n";
        YamlField sideCarsYamlField;
        try {
            String yamlFieldWithUuid = YamlUtils.injectUuid(yamlField);
            sideCarsYamlField = YamlUtils.readTree(yamlFieldWithUuid);
        } catch (IOException e) {
            throw new InvalidRequestException("Exception while creating primary field");
        }
        return sideCarsYamlField.getNode().getCurrJsonNode();
    }


    public YamlField createIndividualSideCarsArtifactYamlFieldAndSetYamlUpdate(
            YamlField sideCarsYamlField, String sideCarIdentifier, Map<String, YamlNode> mapIdentifierWithYamlNode, YamlUpdates.Builder yamlUpdates) {
        if(mapIdentifierWithYamlNode.containsKey(sideCarIdentifier)){
            return new YamlField(mapIdentifierWithYamlNode.get(sideCarIdentifier)).getNode().getField(YamlTypes.SIDECAR_ARTIFACT_CONFIG);
        }
        //TODO: NEED TO ADD CORRECT PARENT NODE
        YamlField individualSideCarYamlField =  new YamlField("[2]",
                new YamlNode(
                        "[2]", SideCarsListArtifactsUtility.getIndividualSideCarsListJsonNode(sideCarIdentifier), sideCarsYamlField.getNode()));
        setYamlUpdate(sideCarsYamlField,yamlUpdates);
        return individualSideCarYamlField;
    }

    public YamlField createSideCarsArtifactYamlFieldAndSetYamlUpdate(
            YamlField artifactField, YamlUpdates.Builder yamlUpdates) {
        YamlField sideCarsYamlField = artifactField.getNode().getField(YamlTypes.SIDECARS_ARTIFACT_CONFIG);

        if (sideCarsYamlField != null) {
            return sideCarsYamlField;
        }

        sideCarsYamlField = createSideCarsYamlFieldUnderArtifacts(artifactField);
        setYamlUpdate(sideCarsYamlField, yamlUpdates);
        return sideCarsYamlField;
    }

    private static YamlUpdates.Builder setYamlUpdate(YamlField yamlField, YamlUpdates.Builder yamlUpdates) {
        try {
            return yamlUpdates.putFqnToYaml(yamlField.getYamlPath(), YamlUtils.writeYamlString(yamlField));
        } catch (IOException e) {
            throw new YamlException(
                    "Yaml created for yamlField at " + yamlField.getYamlPath() + " could not be converted into a yaml string");
        }
    }

    private YamlField createSideCarsYamlFieldUnderArtifacts(YamlField artifacts) {
        return new YamlField(YamlTypes.SIDECARS_ARTIFACT_CONFIG,
                new YamlNode(
                        YamlTypes.SIDECARS_ARTIFACT_CONFIG, SideCarsListArtifactsUtility.getSideCarsListJsonNode(), artifacts.getNode()));
    }
}