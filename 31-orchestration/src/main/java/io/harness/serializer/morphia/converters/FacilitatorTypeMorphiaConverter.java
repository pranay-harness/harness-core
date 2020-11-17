package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.facilitators.FacilitatorType;

public class FacilitatorTypeMorphiaConverter extends ProtoMessageConverter<FacilitatorType> {
  public FacilitatorTypeMorphiaConverter() {
    super(FacilitatorType.class);
  }
}