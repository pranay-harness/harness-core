package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.ambiance.Ambiance;

import com.google.inject.Singleton;

@Singleton
public class AmbianceMorphiaConverter extends ProtoMessageConverter<Ambiance> {
  public AmbianceMorphiaConverter() {
    super(Ambiance.class);
  }
}
