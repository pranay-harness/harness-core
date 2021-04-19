package io.harness.configManager;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

import com.google.inject.Inject;

public class ConfigChangeIterator implements Handler<Configuration> {
  @Inject private ConfigurationController configurationController;

  @Override
  public void handle(Configuration entity) {
    configurationController.run();
  }
}
