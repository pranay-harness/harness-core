package io.harness.serializer.spring.converters.facilitators.obtainment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class FacilitatorObtainmentReadConverter extends ProtoReadConverter<FacilitatorObtainment> {
  public FacilitatorObtainmentReadConverter() {
    super(FacilitatorObtainment.class);
  }
}
