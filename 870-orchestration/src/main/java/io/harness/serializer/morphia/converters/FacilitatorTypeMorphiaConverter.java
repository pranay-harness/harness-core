package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.facilitators.FacilitatorType;

import com.google.inject.Singleton;

@Singleton
public class FacilitatorTypeMorphiaConverter extends ProtoMessageConverter<FacilitatorType> {
  public FacilitatorTypeMorphiaConverter() {
    super(FacilitatorType.class);
  }
}
