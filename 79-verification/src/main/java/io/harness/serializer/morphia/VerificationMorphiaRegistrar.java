package io.harness.serializer.morphia;

import io.harness.entities.AnomalousLogRecord;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class VerificationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(TimeSeriesAnomaliesRecord.class);
    set.add(TimeSeriesCumulativeSums.class);
    set.add(AnomalousLogRecord.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {}
}
