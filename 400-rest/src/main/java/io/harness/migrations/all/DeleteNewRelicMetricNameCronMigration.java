package io.harness.migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class DeleteNewRelicMetricNameCronMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "quartz_verification_jobs");
    collection.findAndRemove(new BasicDBObject("keyName", "NEW_RELIC_METRIC_NAME_COLLECT_CRON"));

    collection = wingsPersistence.getCollection(DEFAULT_STORE, "quartz_verification_triggers");
    collection.findAndRemove(new BasicDBObject("keyName", "NEW_RELIC_METRIC_NAME_COLLECT_CRON"));
  }
}
