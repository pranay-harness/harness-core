package io.harness.testlib;

import com.google.inject.Module;

import io.harness.springdata.PersistenceModule;
import io.harness.testlib.module.TestMongoModule;

public abstract class PersistenceTestModule extends PersistenceModule {
  @Override
  protected Module getMongoModule() {
    return TestMongoModule.getInstance();
  }
}
