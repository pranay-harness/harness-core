package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.dl.WingsPersistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Slf4j
public class AddValidUntilToDelegate implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(Delegate.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<Delegate> delegateInstances = new HIterator<>(wingsPersistence.createQuery(Delegate.class)
                                                                     .field(DelegateKeys.validUntil)
                                                                     .doesNotExist()
                                                                     .project(DelegateKeys.lastHeartBeat, true)
                                                                     .fetch())) {
      while (delegateInstances.hasNext()) {
        final Delegate delegateInstance = delegateInstances.next();
        final ZonedDateTime zonedDateTime =
            ZonedDateTime.ofInstant(OffsetDateTime.now().plusDays(Delegate.TTL.toDays()).toInstant(), ZoneOffset.UTC);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(Delegate.class)
                      .filter(DelegateKeys.uuid, delegateInstance.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject(
                "$set", new BasicDBObject("validUntil", java.util.Date.from(zonedDateTime.toInstant()))));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
