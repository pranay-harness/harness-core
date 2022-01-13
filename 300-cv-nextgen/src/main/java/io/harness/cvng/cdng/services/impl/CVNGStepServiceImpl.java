/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.impl;

import io.harness.common.NGExpressionUtils;
import io.harness.cvng.cdng.beans.CVNGStepType;
import io.harness.cvng.cdng.services.api.CVNGStepService;
import io.harness.pms.merger.helpers.InputSetTemplateHelper;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CVNGStepServiceImpl implements CVNGStepService {
  @Override
  public String getUpdatedInputSetTemplate(String pipelineYaml) {
    try {
      YamlField pipeline = YamlUtils.readTree(pipelineYaml);
      for (YamlNode stage : getStageYamlNodes(pipeline)) {
        updateStageYamlWithDummyInputParam(stage.getField("stage").getNode());
      }
      String updatedPipelineYaml = YamlUtils.writeYamlString(pipeline);
      String updatedTemplate = InputSetTemplateHelper.createTemplateFromPipeline(updatedPipelineYaml);
      if (updatedTemplate != null) {
        YamlField updatedTemplateField = YamlUtils.readTree(updatedTemplate);
        removeDummyFieldFromTemplate(updatedTemplateField);
        return YamlUtils.writeYamlString(updatedTemplateField);
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void removeDummyFieldFromTemplate(YamlField yamlField) {
    if (CVNGStepType.CVNG_VERIFY.getDisplayName().equals(yamlField.getNode().getType())) {
      ((ObjectNode) yamlField.getNode().getCurrJsonNode()).remove("dummyInputParam");
    } else {
      for (YamlField child : yamlField.getNode().fields()) {
        removeDummyFieldFromTemplate(child);
      }
      if (yamlField.getNode().isArray()) {
        for (YamlNode child : yamlField.getNode().asArray()) {
          removeDummyFieldFromTemplate(new YamlField(child));
        }
      }
    }
  }

  @NotNull
  private List<YamlNode> getStageYamlNodes(YamlField pipeline) {
    return pipeline.getNode().getField("pipeline").getNode().getField("stages").getNode().asArray();
  }

  private void updateStageYamlWithDummyInputParam(YamlNode stageYaml) {
    if (!isDeploymentStage(stageYaml)) {
      return;
    }
    String serviceIdentifier = CVNGStepUtils.getServiceRefNode(stageYaml).asText();
    String envIdentifier = CVNGStepUtils.getEnvRefNode(stageYaml).asText();
    if (NGExpressionUtils.matchesInputSetPattern(serviceIdentifier)
        || NGExpressionUtils.matchesInputSetPattern(envIdentifier)) {
      addDummyInputFieldToVerifyStep(CVNGStepUtils.getExecutionNodeField(stageYaml));
    }
  }

  private boolean isDeploymentStage(YamlNode stageYaml) {
    if (stageYaml.getField("type") == null) {
      return false;
    }
    return stageYaml.getField("type").getNode().asText().equals("Deployment");
  }

  private void addDummyInputFieldToVerifyStep(YamlField yamlField) {
    if (CVNGStepType.CVNG_VERIFY.getDisplayName().equals(yamlField.getNode().getType())) {
      if (!hasInputSetPattern(yamlField)) {
        ((ObjectNode) yamlField.getNode().getCurrJsonNode()).put("dummyInputParam", "<+input>");
      }
    } else {
      for (YamlField child : yamlField.getNode().fields()) {
        addDummyInputFieldToVerifyStep(child);
      }
      if (yamlField.getNode().isArray()) {
        for (YamlNode child : yamlField.getNode().asArray()) {
          addDummyInputFieldToVerifyStep(new YamlField(child));
        }
      }
    }
  }

  private boolean hasInputSetPattern(YamlField yamlField) {
    if (NGExpressionUtils.matchesInputSetPattern(yamlField.getNode().asText())) {
      return true;
    } else {
      boolean hasInputParams = false;
      for (YamlField child : yamlField.getNode().fields()) {
        hasInputParams |= hasInputSetPattern(child);
      }
      if (yamlField.getNode().isArray()) {
        for (YamlNode child : yamlField.getNode().asArray()) {
          hasInputParams |= hasInputSetPattern(new YamlField(child));
        }
      }
      return hasInputParams;
    }
  }
}
