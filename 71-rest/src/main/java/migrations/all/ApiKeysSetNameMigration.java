package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;

import com.google.inject.Inject;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.ApiKeyEntry;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ApiKeyService;

/**
 * @author rktummala on 08/23/19
 *
 */
@Slf4j
public class ApiKeysSetNameMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;
  @Inject ApiKeyService apiKeyService;

  @Override
  public void migrate() {
    DBCollection collection = wingsPersistence.getCollection(ApiKeyEntry.class);
    DBCursor apiKeys = collection.find();

    logger.info("will go through " + apiKeys.size() + " api keys");

    while (apiKeys.hasNext()) {
      DBObject next = apiKeys.next();

      String uuId = (String) next.get("_id");
      String name = (String) next.get("name");
      String accountId = (String) next.get("accountId");
      if (isNotEmpty(name)) {
        continue;
      }

      ApiKeyEntry apiKeyEntry = apiKeyService.get(uuId, accountId);
      UpdateOperations<ApiKeyEntry> operations = wingsPersistence.createUpdateOperations(ApiKeyEntry.class);
      setUnset(operations, "name", apiKeyEntry.getDecryptedKey().substring(0, 5));
      wingsPersistence.update(wingsPersistence.createQuery(ApiKeyEntry.class).filter("_id", uuId).get(), operations);
      logger.info("updated api key: " + uuId);
    }

    logger.info("Completed setting name to api keys");
  }
}
