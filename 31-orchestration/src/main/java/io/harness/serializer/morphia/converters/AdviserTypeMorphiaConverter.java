package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.advisers.AdviserType;

import com.google.inject.Singleton;

@Singleton
public class AdviserTypeMorphiaConverter extends ProtoMessageConverter<AdviserType> {
  public AdviserTypeMorphiaConverter() {
    super(AdviserType.class);
  }
}
