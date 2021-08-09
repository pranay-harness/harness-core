package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.plan.SdkModuleInfo;

public class SdkModuleInfoMorphiaConverter extends ProtoMessageConverter<SdkModuleInfo> {
  public SdkModuleInfoMorphiaConverter() {
    super(SdkModuleInfo.class);
  }
}
