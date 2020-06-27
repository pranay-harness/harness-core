package io.harness.testlib;

import com.google.inject.Module;

import io.harness.ng.PersistenceModule;
import io.harness.testlib.module.TestMongoModule;

public abstract class PersistenceTestModule extends PersistenceModule {
  @Override
  protected Module getMongoModule() {
    return new TestMongoModule();
  }
}
