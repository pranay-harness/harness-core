package io.harness.batch.processing.tasklet.util;

import io.harness.batch.processing.entities.InstanceData;
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
}
