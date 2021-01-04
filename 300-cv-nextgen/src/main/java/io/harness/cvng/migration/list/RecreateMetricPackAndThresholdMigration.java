package io.harness.cvng.migration.list;

import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.migration.CNVGMigration;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;

@Slf4j
public class RecreateMetricPackAndThresholdMigration implements CNVGMigration {
  @Inject private HPersistence hPersistence;

  @Override
  public void migrate() {
    log.info("dropping collection {}", MetricPack.class.getAnnotation(Entity.class).value());
    hPersistence.getCollection(MetricPack.class).drop();
    log.info("dropping collection {}", TimeSeriesThreshold.class.getAnnotation(Entity.class).value());
    hPersistence.getCollection(TimeSeriesThreshold.class).drop();
    log.info("migration done");
  }
}
