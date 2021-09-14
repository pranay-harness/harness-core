/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.activityhistory.impl.NGActivityServiceImpl;
import io.harness.ng.core.activityhistory.impl.NGActivitySummaryServiceImpl;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.ng.core.activityhistory.service.NGActivitySummaryService;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.environment.services.impl.EnvironmentServiceImpl;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.ng.core.service.services.impl.ServiceEntityServiceImpl;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(HarnessTeam.PL)
public class NGCoreModule extends AbstractModule {
  private static final AtomicReference<NGCoreModule> instanceRef = new AtomicReference<>();

  public static NGCoreModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGCoreModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    super.configure();
    bind(EnvironmentService.class).to(EnvironmentServiceImpl.class);
    bind(ServiceEntityService.class).to(ServiceEntityServiceImpl.class);
    bind(NGActivityService.class).to(NGActivityServiceImpl.class);
    bind(NGActivitySummaryService.class).to(NGActivitySummaryServiceImpl.class);
  }
}
