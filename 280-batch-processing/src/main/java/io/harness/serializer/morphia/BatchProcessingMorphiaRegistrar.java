package io.harness.serializer.morphia;

import io.harness.batch.processing.billing.tasklet.entities.DataGeneratedNotification;
import io.harness.batch.processing.entities.AccountShardMapping;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class BatchProcessingMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(AccountShardMapping.class);
    set.add(InstanceData.class);
    set.add(DataGeneratedNotification.class);
    // TODO: add the batch processing morphia classes here
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
