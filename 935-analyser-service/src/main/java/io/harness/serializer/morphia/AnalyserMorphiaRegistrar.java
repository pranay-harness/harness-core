package io.harness.serializer.morphia;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryRecordEntity;
import io.harness.event.QueryStats;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.serviceinfo.ServiceInfo;

import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class AnalyserMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(ServiceInfo.class);
    set.add(QueryRecordEntity.class);
    set.add(QueryStats.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
