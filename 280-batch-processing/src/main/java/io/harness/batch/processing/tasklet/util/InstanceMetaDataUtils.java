package io.harness.batch.processing.tasklet.util;

import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@UtilityClass
@Slf4j
public class InstanceMetaDataUtils {
  public static String getValueForKeyFromInstanceMetaData(String metaDataKey, InstanceData instanceData) {
    return getValueForKeyFromInstanceMetaData(metaDataKey, instanceData.getMetaData());
  }

  public static String getValueForKeyFromInstanceMetaData(String metaDataKey, Map<String, String> metaData) {
    if (null != metaData && metaData.containsKey(metaDataKey)) {
      return metaData.get(metaDataKey);
    }
    return null;
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
