package io.harness.serializer.morphia;

import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.core.services.entities.TimeSeriesThreshold;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class CVNextGenCommonMorphiaRegister implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(MetricPack.class);
    set.add(TimeSeriesThreshold.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
