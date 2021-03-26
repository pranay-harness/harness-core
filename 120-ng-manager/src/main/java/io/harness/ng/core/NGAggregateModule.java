package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.api.AggregateProjectService;
import io.harness.ng.core.api.AggregateUserGroupService;
import io.harness.ng.core.api.impl.AggregateOrganizationServiceImpl;
import io.harness.ng.core.api.impl.AggregateProjectServiceImpl;
import io.harness.ng.core.api.impl.AggregateUserGroupServiceImpl;

import com.google.inject.AbstractModule;

@OwnedBy(PL)
public class NGAggregateModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(AggregateProjectService.class).to(AggregateProjectServiceImpl.class);
    bind(AggregateOrganizationService.class).to(AggregateOrganizationServiceImpl.class);
    bind(AggregateUserGroupService.class).to(AggregateUserGroupServiceImpl.class);
  }
}
