package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;

public class AdviserObtainmentMorphiaConverter extends ProtoMessageConverter<AdviserObtainment> {
  public AdviserObtainmentMorphiaConverter() {
    super(AdviserObtainment.class);
  }
}
