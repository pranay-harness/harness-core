package io.harness.serializer.spring.converters.sdk;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.SdkModuleInfo;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class SdkModuleInfoReadConverter extends ProtoReadConverter<SdkModuleInfo> {
  public SdkModuleInfoReadConverter() {
    super(SdkModuleInfo.class);
  }
}