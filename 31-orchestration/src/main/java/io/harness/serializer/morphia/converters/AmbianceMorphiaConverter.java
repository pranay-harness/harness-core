package io.harness.serializer.morphia.converters;

import com.google.inject.Singleton;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.ambiance.Ambiance;

@Singleton
public class AmbianceMorphiaConverter extends ProtoMessageConverter<Ambiance> {
  public AmbianceMorphiaConverter() {
    super(Ambiance.class);
  }
}
