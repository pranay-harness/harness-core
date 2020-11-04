package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.ambiance.Ambiance;

public class AmbianceMorphiaConverter extends ProtoMessageConverter<Ambiance> {
  public AmbianceMorphiaConverter() {
    super(Ambiance.class);
  }
}
