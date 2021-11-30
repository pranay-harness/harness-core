package io.harness.cvng.core.jobs;

import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;

public class ChangeSourceDemoHandler implements MongoPersistenceIterator.Handler<ChangeSource> {
  @Inject private ChangeSourceService changeSourceService;
  @Override
  public void handle(ChangeSource entity) {
    changeSourceService.generateDemoData(entity);
  }
}
