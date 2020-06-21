package io.harness.connector;

import com.google.inject.AbstractModule;

import io.harness.connector.impl.ConnectorServiceImpl;
import io.harness.connector.services.ConnectorService;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;

public class ConnectorModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HPersistence.class).to(MongoPersistence.class);
    bind(ConnectorService.class).to(ConnectorServiceImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
