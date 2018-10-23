package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AuthToken;
import software.wings.dl.WingsPersistence;

import java.util.Date;

public class AuthTokenTtlMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(RenameReplicationControllerStates.class);

  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "authTokens");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    bulkWriteOperation.find(wingsPersistence.createQuery(AuthToken.class).field("ttl").doesNotExist().getQueryObject())
        .update(new BasicDBObject(
            "$set", new BasicDBObject("ttl", new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))));
    bulkWriteOperation.execute();
  }
}
