package io.harness.serializer.morphia;

import io.harness.cvng.core.services.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.core.services.entities.LogCVConfig;
import io.harness.cvng.core.services.entities.MetricCVConfig;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.core.services.entities.SplunkCVConfig;
import io.harness.cvng.core.services.entities.TimeSeriesRecord;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class CVNextGenCommonMorphiaRegister implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(CVConfig.class);
    set.add(MetricPack.class);
    set.add(TimeSeriesRecord.class);
    set.add(SplunkCVConfig.class);
    set.add(AppDynamicsCVConfig.class);
    set.add(LogCVConfig.class);
    set.add(MetricCVConfig.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    // no classes to register
  }
}
