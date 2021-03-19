package io.harness.audit;

import io.harness.audit.api.AuditService;
import io.harness.audit.api.impl.AuditServiceImpl;

import com.google.inject.AbstractModule;

public class AuditServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new AuditFilterModule());
    bind(AuditService.class).to(AuditServiceImpl.class);
  }
}
