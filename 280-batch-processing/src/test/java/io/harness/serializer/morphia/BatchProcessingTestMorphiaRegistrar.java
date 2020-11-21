package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import software.wings.integration.common.MongoDBTest;
import software.wings.integration.dl.PageRequestTest;

import java.util.Set;

public class BatchProcessingTestMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(PageRequestTest.Dummy.class);
    set.add(MongoDBTest.MongoEntity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // nothing to registrer
  }
}
