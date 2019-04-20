package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
@Slf4j
public class CVCollectionCronFrequencyMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection =
        wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "quartz_verification_triggers");
    final WriteResult result = collection.updateMulti(new BasicDBObject("keyGroup", "METRIC_DATA_PROCESSOR_CRON_GROUP"),
        new BasicDBObject("$set", new BasicDBObject("repeatInterval", Long.valueOf(30000))));

    logger.info("updated {} records", result.getN());
    logger.info("migration done...");
  }
}