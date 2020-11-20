package io.harness.serializer.morphia.converters;

import com.google.inject.Singleton;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;

@Singleton
public class AdviserObtainmentMorphiaConverter extends ProtoMessageConverter<AdviserObtainment> {
  public AdviserObtainmentMorphiaConverter() {
    super(AdviserObtainment.class);
  }
}
