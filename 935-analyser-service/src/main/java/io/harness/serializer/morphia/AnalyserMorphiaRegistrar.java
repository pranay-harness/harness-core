package io.harness.serializer.morphia;

import io.harness.event.QueryRecordEntity;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.serviceinfo.ServiceInfo;

import java.util.Set;

public class AnalyserMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(ServiceInfo.class);
    set.add(QueryRecordEntity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
