package io.harness.ng.core;

import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.api.AggregateProjectService;
import io.harness.ng.core.api.impl.AggregateOrganizationServiceImpl;
import io.harness.ng.core.api.impl.AggregateProjectServiceImpl;

import com.google.inject.AbstractModule;

public class NGAggregateModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(AggregateProjectService.class).to(AggregateProjectServiceImpl.class);
    bind(AggregateOrganizationService.class).to(AggregateOrganizationServiceImpl.class);
  }
}
