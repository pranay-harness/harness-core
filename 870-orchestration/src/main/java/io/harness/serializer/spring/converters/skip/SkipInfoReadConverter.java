package io.harness.serializer.spring.converters.skip;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class SkipInfoReadConverter extends ProtoReadConverter<SkipInfo> {
  public SkipInfoReadConverter() {
    super(SkipInfo.class);
  }
}
