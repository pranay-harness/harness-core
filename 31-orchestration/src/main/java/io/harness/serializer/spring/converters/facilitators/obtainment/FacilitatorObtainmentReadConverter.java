package io.harness.serializer.spring.converters.facilitators.obtainment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.facilitators.FacilitatorObtainment;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@ReadingConverter
public class FacilitatorObtainmentReadConverter extends ProtoReadConverter<FacilitatorObtainment> {
  public FacilitatorObtainmentReadConverter() {
    super(FacilitatorObtainment.class);
  }
}
