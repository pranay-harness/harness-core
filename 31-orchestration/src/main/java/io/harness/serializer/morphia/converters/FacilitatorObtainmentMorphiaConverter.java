package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.facilitators.FacilitatorObtainment;

public class FacilitatorObtainmentMorphiaConverter extends ProtoMessageConverter<FacilitatorObtainment> {
  public FacilitatorObtainmentMorphiaConverter() {
    super(FacilitatorObtainment.class);
  }
}
