package io.harness.serializer.spring.converters.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.ambiance.Ambiance;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class AmbianceReadConverter extends ProtoReadConverter<Ambiance> {
  public AmbianceReadConverter() {
    super(Ambiance.class);
  }
}
