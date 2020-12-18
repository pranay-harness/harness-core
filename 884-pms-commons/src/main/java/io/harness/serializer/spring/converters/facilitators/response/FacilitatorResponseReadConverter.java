package io.harness.serializer.spring.converters.facilitators.response;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class FacilitatorResponseReadConverter extends ProtoReadConverter<FacilitatorResponseProto> {
  public FacilitatorResponseReadConverter() {
    super(FacilitatorResponseProto.class);
  }
}
