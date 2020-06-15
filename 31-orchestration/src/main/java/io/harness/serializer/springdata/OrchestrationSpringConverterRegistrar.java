package io.harness.serializer.springdata;

import io.harness.beans.converters.SweepingOutputReadMongoConverter;
import io.harness.beans.converters.SweepingOutputWriteMongoConverter;
import io.harness.ng.SpringConverterRegistrar;
import org.springframework.core.convert.converter.Converter;

import java.util.Set;

public class OrchestrationSpringConverterRegistrar implements SpringConverterRegistrar {
  @Override
  public Set<Converter> registerConverters(Set<Converter> converters) {
    converters.add(new SweepingOutputReadMongoConverter());
    converters.add(new SweepingOutputWriteMongoConverter());
    return converters;
  }
}
