package io.harness.ng.core;

import io.harness.ng.core.activityhistory.impl.EntityActivitySummaryServiceImpl;
import io.harness.ng.core.activityhistory.impl.NGActivityServiceImpl;
import io.harness.ng.core.activityhistory.service.EntityActivitySummaryService;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.ng.core.entitysetupusage.impl.EntitySetupUsageServiceImpl;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.environment.services.impl.EnvironmentServiceImpl;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.ng.core.service.services.impl.ServiceEntityServiceImpl;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

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
    bind(EntitySetupUsageService.class).to(EntitySetupUsageServiceImpl.class);
    bind(NGActivityService.class).to(NGActivityServiceImpl.class);
    bind(EntityActivitySummaryService.class).to(EntityActivitySummaryServiceImpl.class);
  }
}
