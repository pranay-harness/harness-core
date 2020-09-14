package io.harness.ng.core;

import com.google.inject.AbstractModule;

import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;

public class CoreModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HPersistence.class).to(MongoPersistence.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
