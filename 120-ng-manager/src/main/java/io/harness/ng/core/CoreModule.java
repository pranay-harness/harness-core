package io.harness.ng.core;

import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;

import com.google.inject.AbstractModule;

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
