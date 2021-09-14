/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.tasklet.util;

import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.ccm.commons.constants.InstanceMetaDataConstants;
import io.harness.ccm.commons.entities.batch.InstanceData;

import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class InstanceMetaDataUtils {
  public static String getValueForKeyFromInstanceMetaData(@NotNull String metaDataKey, InstanceData instanceData) {
    return getValueForKeyFromInstanceMetaData(metaDataKey, instanceData.getMetaData());
  }

  public static String getValueForKeyFromInstanceMetaData(@NotNull String metaDataKey, Map<String, String> metaData) {
    if (null != metaData && metaData.containsKey(metaDataKey)) {
      return metaData.get(metaDataKey);
    }
    return null;
  }

  public static boolean carryUpdatedMapKeyFromTo(Map<String, String> fromMap, Map<String, String> toMap) {
    boolean updateRequired = false;
    if (fromMap != null && toMap != null) {
      for (Map.Entry<String, String> e : fromMap.entrySet()) {
        if (!Objects.equals(e.getValue(), toMap.get(e.getKey()))) {
          toMap.put(e.getKey(), e.getValue());
          updateRequired = true;
        }
      }
    }
    return updateRequired;
  }

  public static void populateNodePoolNameFromLabel(Map<String, String> labelsMap, Map<String, String> metaData) {
    String nodePoolValue = null;
    if (null != labelsMap.get(K8sCCMConstants.GKE_NODE_POOL_KEY)) {
      nodePoolValue = labelsMap.get(K8sCCMConstants.GKE_NODE_POOL_KEY);
    } else if (null != labelsMap.get(K8sCCMConstants.AKS_NODE_POOL_KEY)) {
      nodePoolValue = labelsMap.get(K8sCCMConstants.AKS_NODE_POOL_KEY);
    } else if (null != labelsMap.get(K8sCCMConstants.EKS_NODE_POOL_KEY)) {
      nodePoolValue = labelsMap.get(K8sCCMConstants.EKS_NODE_POOL_KEY);
    }
    if (null != nodePoolValue) {
      metaData.put(InstanceMetaDataConstants.NODE_POOL_NAME, nodePoolValue);
    }
  }
}
