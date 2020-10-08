package io.harness.cvng.core.activity.jobs;

import com.google.inject.Inject;

import io.harness.cvng.core.activity.entities.KubernetesActivitySource;
import io.harness.cvng.core.activity.services.api.KubernetesActivitySourceService;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8ActivityCollectionHandler implements MongoPersistenceIterator.Handler<KubernetesActivitySource> {
  @Inject private KubernetesActivitySourceService activityService;
  @Override
  public void handle(KubernetesActivitySource activitySource) {
    logger.info("Enqueuing activitySource {}", activitySource.getUuid());
    activityService.enqueueDataCollectionTask(activitySource);
    logger.info("Done enqueuing activitySource {}", activitySource.getUuid());
  }
}
