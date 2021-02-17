package io.harness.cvng.core.jobs;

import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.services.api.MonitoringTaskPerpetualTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class MonitoringSourcePerpetualTaskHandler implements Handler<MonitoringSourcePerpetualTask> {
  @Inject private MonitoringTaskPerpetualTaskService monitoringTaskPerpetualTaskService;
  @Override
  public void handle(MonitoringSourcePerpetualTask entity) {
    log.info("Enqueuing monitoring source {}", entity.getUuid());
    monitoringTaskPerpetualTaskService.createPerpetualTask(entity);
    log.info("Done enqueuing monitoring source {}", entity.getUuid());
  }
}
