package io.harness.ng.core;

import com.google.inject.AbstractModule;

import io.harness.mongo.MongoPersistence;
import io.harness.ng.core.services.api.OrganizationService;
import io.harness.ng.core.services.api.ProjectService;
import io.harness.ng.core.services.api.impl.OrganizationServiceImpl;
import io.harness.ng.core.services.api.impl.ProjectServiceImpl;
import io.harness.persistence.HPersistence;

public class CoreModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HPersistence.class).to(MongoPersistence.class);
    bind(ProjectService.class).to(ProjectServiceImpl.class);
    bind(OrganizationService.class).to(OrganizationServiceImpl.class);

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
