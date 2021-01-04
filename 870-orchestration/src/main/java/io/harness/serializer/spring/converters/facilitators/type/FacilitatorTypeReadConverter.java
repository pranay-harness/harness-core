package io.harness.serializer.spring.converters.facilitators.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class FacilitatorTypeReadConverter extends ProtoReadConverter<FacilitatorType> {
  public FacilitatorTypeReadConverter() {
    super(FacilitatorType.class);
  }
}
