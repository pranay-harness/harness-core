package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.ambiance.Level;

public class LevelMorphiaConverter extends ProtoMessageConverter<Level> {
  public LevelMorphiaConverter() {
    super(Level.class);
  }
}
