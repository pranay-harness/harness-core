package io.harness.serializer.spring.converters.facilitators.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.facilitators.FacilitatorType;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@ReadingConverter
public class FacilitatorTypeReadConverter extends ProtoReadConverter<FacilitatorType> {
  public FacilitatorTypeReadConverter() {
    super(FacilitatorType.class);
  }
}
