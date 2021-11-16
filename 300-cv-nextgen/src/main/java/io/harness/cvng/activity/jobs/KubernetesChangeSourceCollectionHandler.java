package io.harness.cvng.activity.jobs;

import io.harness.cvng.core.entities.changeSource.KubernetesChangeSource;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class KubernetesChangeSourceCollectionHandler
    implements MongoPersistenceIterator.Handler<KubernetesChangeSource> {
  @Inject ChangeSourceService changeSourceService;

  @Override
  public void handle(KubernetesChangeSource kubernetesChangeSource) {
    log.info("Enqueuing changesource {}", kubernetesChangeSource.getIdentifier());
    changeSourceService.enqueueDataCollectionTask(kubernetesChangeSource);
    log.info("Completed change source {}", kubernetesChangeSource.getIdentifier());
  }
}
