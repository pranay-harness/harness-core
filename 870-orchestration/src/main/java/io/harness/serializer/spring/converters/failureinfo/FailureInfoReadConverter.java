package io.harness.serializer.spring.converters.failureinfo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class FailureInfoReadConverter extends ProtoReadConverter<FailureInfo> {
  public FailureInfoReadConverter() {
    super(FailureInfo.class);
  }
}
