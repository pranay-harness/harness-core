package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import io.harness.data.structure.ListUtil;
import migrations.Migration;
import org.mongodb.morphia.query.MorphiaIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

import java.util.List;

public class ServiceKeywordsMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddValidUntilToStateExecutionInstance.class);
  public static final int BATCH_SIZE = 50;

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection("services");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    MorphiaIterator<Service, Service> services =
        wingsPersistence.createQuery(Service.class).project("appContainer", false).fetch();
    int i = 1;
    try (DBCursor ignored = services.getCursor()) {
      while (services.hasNext()) {
        Service service = services.next();
        if (i % BATCH_SIZE == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("Services: {} updated", i);
        }
        ++i;
        List<Object> keywords = service.generateKeywords();
        bulkWriteOperation
            .find(
                wingsPersistence.createQuery(Service.class).filter(Service.ID_KEY, service.getUuid()).getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("keywords", ListUtil.trimList(keywords))));
      }
    }
    if (i % BATCH_SIZE != 1) {
      bulkWriteOperation.execute();
    }
  }
}
