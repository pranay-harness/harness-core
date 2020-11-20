package io.harness.serializer.morphia.converters;

import com.google.inject.Singleton;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.facilitators.FacilitatorObtainment;

@Singleton
public class FacilitatorObtainmentMorphiaConverter extends ProtoMessageConverter<FacilitatorObtainment> {
  public FacilitatorObtainmentMorphiaConverter() {
    super(FacilitatorObtainment.class);
  }
}
