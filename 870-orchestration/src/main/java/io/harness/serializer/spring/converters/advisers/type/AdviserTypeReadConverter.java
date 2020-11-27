package io.harness.serializer.spring.converters.advisers.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.advisers.AdviserType;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class AdviserTypeReadConverter extends ProtoReadConverter<AdviserType> {
  public AdviserTypeReadConverter() {
    super(AdviserType.class);
  }
}
