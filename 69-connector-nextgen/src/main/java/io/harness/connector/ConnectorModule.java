package io.harness.connector;

import com.google.inject.AbstractModule;

import io.harness.connector.impl.ConnectorServiceImpl;
import io.harness.connector.services.ConnectorService;
import io.harness.persistence.HPersistence;

public class ConnectorModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ConnectorService.class).to(ConnectorServiceImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
