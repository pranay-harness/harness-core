package io.harness.testing;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class ComponentTestsModule extends AbstractModule {
  @Override
  protected void configure() {
    MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
  }
}
