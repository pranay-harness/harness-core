package io.harness.serializer.spring.converters.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.ambiance.Ambiance;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@ReadingConverter
public class AmbianceReadConverter extends ProtoReadConverter<Ambiance> {
  public AmbianceReadConverter() {
    super(Ambiance.class);
  }
}
