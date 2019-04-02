package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

public class ServiceNameMigrationIfEmpty implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(ServiceNameMigrationIfEmpty.class);
  public static final String HARNESS_SERVICE = "HARNESS_SERVICE";

  @Inject private WingsPersistence wingsPersistence;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    logger.info("Changing Empty Service Name to Harness_Service");
    final DBCollection collection = wingsPersistence.getCollection(Service.class, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int i = 1;
    int count = 1;
    logger.info("Migrating Services with No Name");
    try (HIterator<Service> services =
             new HIterator<>(wingsPersistence.createQuery(Service.class).project(Service.NAME_KEY, true).fetch())) {
      while (services.hasNext()) {
        Service service = services.next();
        if (i % 50 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("Services: {} updated", i);
        }
        if (isEmpty(service.getName()) || isEmpty(service.getName().trim())) {
          bulkWriteOperation
              .find(wingsPersistence.createQuery(Service.class)
                        .filter(Service.ID_KEY, service.getUuid())
                        .getQueryObject())
              .updateOne(new BasicDBObject("$set",
                  new BasicDBObject(Service.NAME_KEY,
                      new StringBuilder(128).append(HARNESS_SERVICE).append("_0").append(count).toString())));
          ++count;
          ++i;
        }
      }
    }
    if (i % 50 != 1) {
      bulkWriteOperation.execute();
      logger.info("Services: {} updated", i);
    }
    logger.info("Migrating Empty Service Names completed");
  }
}
