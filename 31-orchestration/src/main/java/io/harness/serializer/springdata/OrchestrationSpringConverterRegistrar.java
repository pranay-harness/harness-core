package io.harness.serializer.springdata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.converters.SweepingOutputReadMongoConverter;
import io.harness.beans.converters.SweepingOutputWriteMongoConverter;
import io.harness.ng.SpringConverterRegistrar;
import org.springframework.core.convert.converter.Converter;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationSpringConverterRegistrar implements SpringConverterRegistrar {
  @Override
  public Set<Converter> registerConverters(Set<Converter> converters) {
    converters.add(new SweepingOutputReadMongoConverter());
    converters.add(new SweepingOutputWriteMongoConverter());
    return converters;
  }
}
