package io.harness.serializer.morphia.converters;

import com.google.inject.Singleton;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.advisers.AdviserType;

@Singleton
public class AdviserTypeMorphiaConverter extends ProtoMessageConverter<AdviserType> {
  public AdviserTypeMorphiaConverter() {
    super(AdviserType.class);
  }
}
