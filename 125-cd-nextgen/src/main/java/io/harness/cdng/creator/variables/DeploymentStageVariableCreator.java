package io.harness.cdng.creator.variables;

import io.harness.cdng.visitor.YamlTypes;
import io.harness.pms.sdk.core.pipeline.variables.VariableCreatorHelper;
import io.harness.pms.sdk.core.variables.AbstractStageVariableCreator;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DeploymentStageVariableCreator extends AbstractStageVariableCreator {
  @Override
  public LinkedHashMap<String, VariableCreationResponse> createVariablesForChildrenNodes(
      VariableCreationContext ctx, YamlField config) {
    LinkedHashMap<String, VariableCreationResponse> responseMap = new LinkedHashMap<>();
    YamlField serviceField =
        config.getNode().getField(YAMLFieldNameConstants.SPEC).getNode().getField(YamlTypes.SERVICE_CONFIG);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(serviceField)) {
      VariableCreationResponse serviceVariableResponse = ServiceVariableCreator.createVariableResponse(serviceField);
      responseMap.put(serviceField.getNode().getUuid(), serviceVariableResponse);
    }

    YamlField infraField =
        config.getNode().getField(YAMLFieldNameConstants.SPEC).getNode().getField(YamlTypes.PIPELINE_INFRASTRUCTURE);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(infraField)) {
      VariableCreationResponse infraVariableResponse = InfraVariableCreator.createVariableResponse(infraField);
      responseMap.put(infraField.getNode().getUuid(), infraVariableResponse);
    }

    YamlField executionField =
        config.getNode().getField(YAMLFieldNameConstants.SPEC).getNode().getField(YAMLFieldNameConstants.EXECUTION);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(executionField)) {
      responseMap.put(executionField.getNode().getUuid(),
          VariableCreationResponse.builder().dependency(executionField.getNode().getUuid(), executionField).build());
    }

    return responseMap;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.STAGE, Collections.singleton("Deployment"));
  }
}
