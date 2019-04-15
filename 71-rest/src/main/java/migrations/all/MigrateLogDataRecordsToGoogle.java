package migrations.all;

import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.intfc.DataStoreService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MigrateLogDataRecordsToGoogle implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DataStoreService dataStoreService;

  @Override
  public void migrate() {
    if (dataStoreService instanceof MongoDataStoreServiceImpl) {
      logger.info("Datastore service is an instance of MongoDataStoreServiceImpl. Not migrating the records now.");
      return;
    }
    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.DAYS.toMillis(7);

    List<LogDataRecord> recordsFromMongo = new ArrayList<>();

    Query<LogDataRecord> logDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                                  .filter("clusterLevel", "L2")
                                                  .field("createdAt")
                                                  .greaterThan(startTime);

    try (HIterator<LogDataRecord> records = new HIterator<>(logDataRecordQuery.fetch())) {
      while (records.hasNext()) {
        recordsFromMongo.add(records.next());
        if (recordsFromMongo.size() == 1000) {
          dataStoreService.save(LogDataRecord.class, recordsFromMongo, true);
          logger.info("Copied 1000 L2 records from Mongo to GoogleDataStore");
          recordsFromMongo = new ArrayList<>();
          sleep(ofMillis(1500));
        }
      }
    }
    dataStoreService.save(LogDataRecord.class, recordsFromMongo, true);
    logger.info("Copied {} L2 records from Mongo to GoogleDataStore", recordsFromMongo.size());
  }
}
