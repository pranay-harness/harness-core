package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;

/**
 * Migration job for VerificationMetric job.
 * The job is supposed to run every minute.
 *
 * Created by Pranjal on 03/18/2019
 */
public class VerificationMetricJobMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(VerificationMetricJobMigration.class);
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection =
        wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "quartz_verification_triggers");
    final WriteResult result = collection.updateMulti(new BasicDBObject("keyGroup", "VERIFICATION_METRIC_CRON_GROUP"),
        new BasicDBObject("$set", new BasicDBObject("repeatInterval", Long.valueOf(60000))));
    logger.info("updated {} records", result.getN());
    logger.info("VerificationMetricJob Migration Completed");
  }
}
