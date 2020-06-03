package io.harness.serializer.morphia;

import io.harness.entities.AnomalousLogRecord;
import io.harness.entities.CVTask;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Set;

public class VerificationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(TimeSeriesAnomaliesRecord.class);
    set.add(TimeSeriesCumulativeSums.class);
    set.add(AnomalousLogRecord.class);
    set.add(CVTask.class);
  }

  @Override
  public void registerImplementationClasses(HelperPut h, HelperPut w) {
    // no classes to register
  }
}
