package io.harness.ng.core;

import com.google.inject.AbstractModule;

import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.environment.services.impl.EnvironmentServiceImpl;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.ng.core.service.services.impl.ServiceEntityServiceImpl;

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
  }
}
