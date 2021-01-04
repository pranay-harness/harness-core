package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;

import com.google.inject.Singleton;

@Singleton
public class FacilitatorObtainmentMorphiaConverter extends ProtoMessageConverter<FacilitatorObtainment> {
  public FacilitatorObtainmentMorphiaConverter() {
    super(FacilitatorObtainment.class);
  }
}
