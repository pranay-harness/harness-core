package io.harness.serializer.spring.converters.advisers.obtainment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.advisers.AdviserObtainment;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class AdviserObtainmentReadConverter extends ProtoReadConverter<AdviserObtainment> {
  public AdviserObtainmentReadConverter() {
    super(AdviserObtainment.class);
  }
}
