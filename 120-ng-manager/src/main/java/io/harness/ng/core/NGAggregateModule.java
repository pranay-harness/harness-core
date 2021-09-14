/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.user.service.impl.NgUserServiceImpl.THREAD_POOL_NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.manage.ManagedExecutorService;
import io.harness.ng.core.api.AggregateAccountResourceService;
import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.api.AggregateProjectService;
import io.harness.ng.core.api.AggregateUserGroupService;
import io.harness.ng.core.api.impl.AggregateAccountResourceServiceImpl;
import io.harness.ng.core.api.impl.AggregateOrganizationServiceImpl;
import io.harness.ng.core.api.impl.AggregateProjectServiceImpl;
import io.harness.ng.core.api.impl.AggregateUserGroupServiceImpl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OwnedBy(PL)
public class NGAggregateModule extends AbstractModule {
  @Override
  protected void configure() {
    int poolSize = Runtime.getRuntime().availableProcessors() * 3;
    bind(ExecutorService.class)
        .annotatedWith(Names.named("aggregate-projects"))
        .toInstance(new ManagedExecutorService(Executors.newFixedThreadPool(poolSize)));
    bind(ExecutorService.class)
        .annotatedWith(Names.named("aggregate-orgs"))
        .toInstance(new ManagedExecutorService(Executors.newFixedThreadPool(poolSize)));
    bind(ExecutorService.class)
        .annotatedWith(Names.named(THREAD_POOL_NAME))
        .toInstance(new ManagedExecutorService(Executors.newFixedThreadPool(poolSize)));
    bind(AggregateProjectService.class).to(AggregateProjectServiceImpl.class);
    bind(AggregateOrganizationService.class).to(AggregateOrganizationServiceImpl.class);
    bind(AggregateAccountResourceService.class).to(AggregateAccountResourceServiceImpl.class);
    bind(AggregateUserGroupService.class).to(AggregateUserGroupServiceImpl.class);
  }
}
