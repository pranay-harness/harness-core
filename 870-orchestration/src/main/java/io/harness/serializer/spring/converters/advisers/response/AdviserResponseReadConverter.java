package io.harness.serializer.spring.converters.advisers.response;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class AdviserResponseReadConverter extends ProtoReadConverter<AdviserResponse> {
  public AdviserResponseReadConverter() {
    super(AdviserResponse.class);
  }
}
