package io.harness.batch.processing.processor.util;

import io.harness.batch.processing.entities.InstanceData;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class InstanceMetaDataUtils {
  public String getValueForKeyFromInstanceMetaData(String metaDataKey, InstanceData instanceData) {
    if (null != instanceData.getMetaData() && instanceData.getMetaData().containsKey(metaDataKey)) {
      return instanceData.getMetaData().get(metaDataKey);
    }
    return null;
  }
}
