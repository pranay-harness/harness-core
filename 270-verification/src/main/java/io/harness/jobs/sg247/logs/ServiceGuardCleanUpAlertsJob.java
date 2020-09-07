package io.harness.jobs.sg247.logs;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.alert.Alert;
import software.wings.dl.WingsPersistence;

@Slf4j
public class ServiceGuardCleanUpAlertsJob implements MongoPersistenceIterator.Handler<Alert> {
  // TODO: Remove this iterator when the alerts with configurable TTL is available.:
  // https://harness.atlassian.net/browse/PL-8790
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void handle(Alert alert) {
    if (alert != null) {
      logger.info("Deleting service guard alert with UUID: {}", alert.getUuid());
      wingsPersistence.delete(Alert.class, alert.getUuid());
    }
  }
}
