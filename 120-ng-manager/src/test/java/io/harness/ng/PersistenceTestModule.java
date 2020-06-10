package io.harness.ng;

import com.google.inject.Module;

import io.harness.testlib.module.TestMongoModule;

public class PersistenceTestModule extends PersistenceModule {
  @Override
  protected Module getMongoModule() {
    return new TestMongoModule();
  }
}
