package io.harness.serializer.morphia;

import io.harness.entities.AnomalousLogRecord;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.mongo.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class VerificationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Set<Class> set) {
    set.add(TimeSeriesAnomaliesRecord.class);
    set.add(TimeSeriesCumulativeSums.class);
    set.add(AnomalousLogRecord.class);
  }

  @Override
  public void register(Map<String, Class> map) {}
}
