package io.harness.serializer.morphia.converters;

import com.google.inject.Singleton;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.ambiance.Level;

@Singleton
public class LevelMorphiaConverter extends ProtoMessageConverter<Level> {
  public LevelMorphiaConverter() {
    super(Level.class);
  }
}
