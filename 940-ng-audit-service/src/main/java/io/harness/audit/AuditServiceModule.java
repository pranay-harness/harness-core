package io.harness.audit;

import com.google.inject.AbstractModule;

public class AuditServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new AuditFilterModule());
  }
}
