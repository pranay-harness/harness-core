package migrations.all;

import static io.harness.data.structure.ListUtils.trimStrings;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

import java.util.List;

@Slf4j
public class AppKeywordsMigration implements Migration {
  public static final int BATCH_SIZE = 50;

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(Application.class, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int i = 1;
    try (HIterator<Application> apps = new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      while (apps.hasNext()) {
        Application application = apps.next();
        if (i % BATCH_SIZE == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("Applications: {} updated", i);
        }
        ++i;
        List<String> keywords = application.generateKeywords();
        bulkWriteOperation
            .find(wingsPersistence.createQuery(Service.class)
                      .filter(Service.ID_KEY, application.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("keywords", trimStrings(keywords))));
      }
    }
    if (i % BATCH_SIZE != 1) {
      bulkWriteOperation.execute();
    }
  }
}
