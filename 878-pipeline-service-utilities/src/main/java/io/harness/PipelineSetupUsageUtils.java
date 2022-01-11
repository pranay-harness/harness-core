/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness;

import io.harness.beans.IdentifierRef;
import io.harness.common.NGExpressionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNMapGenerator;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.preflight.PreFlightCheckMetadata;
import io.harness.utils.IdentifierRefHelper;

import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PipelineSetupUsageUtils {
  public List<EntityDetail> extractInputReferredEntityFromYaml(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String pipelineYaml, List<EntitySetupUsageDTO> allReferredUsages) {
    Map<String, Object> fqnToObjectMapMergedYaml = new HashMap<>();
    try {
      Map<FQN, Object> fqnObjectMap =
          FQNMapGenerator.generateFQNMap(YamlUtils.readTree(pipelineYaml).getNode().getCurrJsonNode());
      fqnObjectMap.keySet().forEach(fqn -> fqnToObjectMapMergedYaml.put(fqn.getExpressionFqn(), fqnObjectMap.get(fqn)));
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid merged pipeline yaml");
    }

    List<EntityDetail> entityDetails = new ArrayList<>();
    for (EntitySetupUsageDTO referredUsage : allReferredUsages) {
      IdentifierRef ref = (IdentifierRef) referredUsage.getReferredEntity().getEntityRef();
      Map<String, String> metadata = ref.getMetadata();
      if (metadata == null) {
        continue;
      }
      String fqn = metadata.get(PreFlightCheckMetadata.FQN);

      if (!metadata.containsKey(PreFlightCheckMetadata.EXPRESSION)) {
        entityDetails.add(referredUsage.getReferredEntity());
      } else if (fqnToObjectMapMergedYaml.containsKey(fqn)) {
        String finalValue = ((TextNode) fqnToObjectMapMergedYaml.get(fqn)).asText();
        if (NGExpressionUtils.isRuntimeOrExpressionField(finalValue)) {
          continue;
        }
        if (ParameterField.containsInputSetValidator(finalValue)) {
          finalValue = ParameterField.getValueFromParameterFieldWithInputSetValidator(finalValue);
        }
        IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
            finalValue, accountIdentifier, orgIdentifier, projectIdentifier, metadata);
        entityDetails.add(EntityDetail.builder()
                              .name(referredUsage.getReferredEntity().getName())
                              .type(referredUsage.getReferredEntity().getType())
                              .entityRef(identifierRef)
                              .build());
      }
    }
    return entityDetails;
  }
}
