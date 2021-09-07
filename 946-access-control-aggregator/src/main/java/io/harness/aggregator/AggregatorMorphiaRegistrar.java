package io.harness.aggregator;

import io.harness.aggregator.models.AggregatorSecondarySyncState;
import io.harness.aggregator.models.MongoReconciliationOffset;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

@OwnedBy(HarnessTeam.PL)
public class AggregatorMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(AggregatorSecondarySyncState.class);
    set.add(MongoReconciliationOffset.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}