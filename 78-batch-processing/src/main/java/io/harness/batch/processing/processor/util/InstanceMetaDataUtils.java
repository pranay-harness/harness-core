package io.harness.batch.processing.processor.util;

import io.harness.batch.processing.entities.InstanceData;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@UtilityClass
@Slf4j
public class InstanceMetaDataUtils {
  public String getValueForKeyFromInstanceMetaData(String metaDataKey, InstanceData instanceData) {
    return getValueForKeyFromInstanceMetaData(metaDataKey, instanceData.getMetaData());
  }

  public String getValueForKeyFromInstanceMetaData(String metaDataKey, Map<String, String> metaData) {
    if (null != metaData && metaData.containsKey(metaDataKey)) {
      return metaData.get(metaDataKey);
    }
    return null;
  }
}
