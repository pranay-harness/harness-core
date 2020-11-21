package software.wings.dl.exportimport;

import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;

import software.wings.dl.WingsPersistence;
import software.wings.dl.exportimport.ImportStatusReport.ImportStatus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.DBCollectionFindOptions;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;

/**
 * @author marklu on 10/24/18
 */
@Slf4j
@Singleton
public class WingsMongoExportImport {
  private static final int BATCH_SIZE = 1000;

  @Inject private WingsPersistence wingsPersistence;

  /**
   * Export the collection object matching the query filter into raw JSON documents.
   */
  public List<String> exportRecords(DBObject filter, String collectionName) {
    final List<String> records = new ArrayList<>();
    DBCollection collection =
        wingsPersistence.getDatastore(HPersistence.DEFAULT_STORE).getDB().getCollection(collectionName);

    DBCursor cursor = collection.find(filter, new DBCollectionFindOptions().batchSize(BATCH_SIZE));
    while (cursor.hasNext()) {
      BasicDBObject basicDBObject = (BasicDBObject) cursor.next();
      records.add(basicDBObject.toJson());
    }

    return records;
  }

  /**
   * Import raw JSON data records into existing mongo collection using the specified import mode. If needed,
   * the 'naturalKeyFields' besides '_id' field will be used to identify if there is pre-existing record in
   * the same collection already. Depending on the import mode, different action will be taken to handle
   * pre-existing records.
   *
   * @param  mode one of the supported import mode such as DRY_RUN/UPSERT etc.
   */
  public ImportStatus importRecords(String collectionName, List<String> records, ImportMode mode) {
    DBCollection collection =
        wingsPersistence.getDatastore(HPersistence.DEFAULT_STORE).getDB().getCollection(collectionName);

    int totalRecords = records.size();
    int importedRecords = 0;
    int idClashCount = 0;
    for (String record : records) {
      // Check for if there is any existing record with the same _id.
      DBObject importRecord = BasicDBObject.parse(record);
      Object id = importRecord.get("_id");
      long recordCountFromId = collection.getCount(new BasicDBObject("_id", id));
      idClashCount += recordCountFromId;

      switch (mode) {
        case DRY_RUN:
          break;
        case INSERT:
          if (recordCountFromId == 0) {
            // Totally new record, it can be inserted directly.
            try {
              collection.insert(importRecord, WriteConcern.ACKNOWLEDGED);
              importedRecords++;
            } catch (DuplicateKeyException e) {
              log.warn("Skip importing a record with conflicting key with existing record in db.", e);
            }
          }
          break;
        case UPSERT:
          // We should not UPSERT record if same ID record exists, but with different natural key.
          try {
            collection.save(importRecord, WriteConcern.ACKNOWLEDGED);
            importedRecords++;
          } catch (DuplicateKeyException e) {
            // PL-2536: Skip conflicting user entries when import account data exported from free into paid cluster.
            log.warn("Skip importing a record with conflicting key with existing record in db.", e);
          }
          break;
        default:
          throw new IllegalArgumentException("Import mode " + mode + " is not supported");
      }
    }

    if (importedRecords + idClashCount > 0) {
      log.info("{} '{}' records have the same ID as existing records.", idClashCount, collectionName);
      log.info("{} out of {} '{}' records have been imported successfully in {} mode.", importedRecords, totalRecords,
          collectionName, mode);
    }
    return ImportStatus.builder()
        .collectionName(collectionName)
        .imported(importedRecords)
        .idClashes(idClashCount)
        .build();
  }

  public static String getCollectionName(Class<? extends PersistentEntity> clazz) {
    return clazz.getAnnotation(Entity.class).value();
  }
}
