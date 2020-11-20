package io.harness.serializer.spring.converters.advisers.obtainment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class AdviserObtainmentReadConverter extends ProtoReadConverter<AdviserObtainment> {
  public AdviserObtainmentReadConverter() {
    super(AdviserObtainment.class);
  }
}
