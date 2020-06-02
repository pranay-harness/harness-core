package io.harness.ng.core;

import com.google.inject.AbstractModule;

import io.harness.ng.core.services.api.ProjectService;
import io.harness.ng.core.services.api.impl.ProjectServiceImpl;
import io.harness.persistence.HPersistence;

public class CoreModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ProjectService.class).to(ProjectServiceImpl.class);

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
