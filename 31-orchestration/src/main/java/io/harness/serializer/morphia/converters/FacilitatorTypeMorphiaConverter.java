package io.harness.serializer.morphia.converters;

import com.google.inject.Singleton;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.facilitators.FacilitatorType;

@Singleton
public class FacilitatorTypeMorphiaConverter extends ProtoMessageConverter<FacilitatorType> {
  public FacilitatorTypeMorphiaConverter() {
    super(FacilitatorType.class);
  }
}