package io.harness.cvng.core.jobs;

import io.harness.cvng.core.entities.demo.CVNGDemoPerpetualTask;
import io.harness.cvng.core.services.api.demo.CVNGDemoPerpetualTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.SneakyThrows;

@Singleton
public class CVNGDemoPerpetualTaskHandler implements MongoPersistenceIterator.Handler<CVNGDemoPerpetualTask> {
  @Inject private CVNGDemoPerpetualTaskService cvngDemoPerpetualTaskService;

  @SneakyThrows
  @Override
  public void handle(CVNGDemoPerpetualTask cvngDemoPerpetualTask) {
    cvngDemoPerpetualTaskService.execute(cvngDemoPerpetualTask);
  }
}
