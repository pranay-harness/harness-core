/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.plan.creation;

import static io.harness.cf.pipeline.FeatureFlagStageFilterJsonCreator.FEATURE_FLAG_SUPPORTED_TYPE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.sdk.PmsSdkInstanceService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NodeTypeLookupServiceImpl implements NodeTypeLookupService {
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;

  @Override
  public String findNodeTypeServiceName(String nodeType) {
    Map<String, Map<String, Set<String>>> map = pmsSdkInstanceService.getInstanceNameToSupportedTypes();
    if (isEmpty(map)) {
      throw new InvalidRequestException("Supported Types Map is empty");
    }
    for (Map.Entry<String, Map<String, Set<String>>> entry : map.entrySet()) {
      Set<String> supportedNodeTypes = new HashSet<>();
      for (Set<String> stringSet : entry.getValue().values()) {
        supportedNodeTypes.addAll(stringSet);
        if (isEmpty(supportedNodeTypes)) {
          continue;
        }

        if (supportedNodeTypes.stream().anyMatch(st -> st.equals(nodeType))) {
          if (nodeType.equals(FEATURE_FLAG_SUPPORTED_TYPE)) {
            return "cf";
          }
          return entry.getKey();
        }
      }
    }

    throw new InvalidRequestException("Unknown Node type: " + nodeType);
  }
}
