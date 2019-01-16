package software.wings.dl.exportimport;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.DBCollectionFindOptions;
import io.harness.data.structure.EmptyPredicate;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsPersistence;
import software.wings.dl.exportimport.ImportStatusReport.ImportStatus;

import java.util.ArrayList;
import java.util.List;

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
    DBCollection collection = wingsPersistence.getDatastore(HPersistence.DEFAULT_STORE, ReadPref.NORMAL)
                                  .getDB()
                                  .getCollection(collectionName);

    DBCursor cursor = collection.find(filter, new DBCollectionFindOptions().batchSize(BATCH_SIZE));
    while (cursor.hasNext()) {
      BasicDBObject basicDBObject = (BasicDBObject) cursor.next();
      records.add(basicDBObject.toJson());
    }

    return records;
  }

  public ImportStatus importRecords(String collectionName, List<String> records, ImportMode mode) {
    return importRecords(collectionName, records, mode, null);
  }

  /**
   * Import raw JSON data records into existing mongo collection using the specified import mode. If needed,
   * the 'naturalKeyFields' besides '_id' field will be used to identify if there is pre-existing record in
   * the same collection already. Depending on the import mode, different action will be taken to handle
   * pre-existing records.
   *
   * @param  mode one of the supported import mode such as DRY_RUN/UPSERT etc.
   */
  public ImportStatus importRecords(
      String collectionName, List<String> records, ImportMode mode, String[] naturalKeyFields) {
    DBCollection collection = wingsPersistence.getDatastore(HPersistence.DEFAULT_STORE, ReadPref.NORMAL)
                                  .getDB()
                                  .getCollection(collectionName);

    int totalRecords = records.size();
    int importedRecords = 0;
    int idClashCount = 0;
    int naturalKeyClashCount = 0;
    for (String record : records) {
      // Check for if there is any existing record with the same _id.
      DBObject importRecord = BasicDBObject.parse(record);
      Object id = importRecord.get("_id");
      long recordCountFromId = collection.getCount(new BasicDBObject("_id", id));

      final DBObject naturalKeyQuery;
      if (EmptyPredicate.isEmpty(naturalKeyFields)) {
        naturalKeyQuery = getDefaultNaturalKeyQuery(importRecord);
      } else {
        naturalKeyQuery = getNaturalKeyQueryFromKeyFields(naturalKeyFields, importRecord);
      }
      long recordCountFromNaturalKey = collection.getCount(naturalKeyQuery);
      if (recordCountFromNaturalKey > 1) {
        // This usually means this entity has no naturaly key. E.g 'secretChangeLogs' collection
        recordCountFromNaturalKey = 1;
      }

      idClashCount += recordCountFromId;
      naturalKeyClashCount += recordCountFromNaturalKey;

      switch (mode) {
        case DRY_RUN:
          break;
        case INSERT:
          if (recordCountFromId == 0 && recordCountFromNaturalKey == 0) {
            // Totally new record, it can be inserted directly.
            collection.insert(importRecord, WriteConcern.ACKNOWLEDGED);
            importedRecords++;
          }
          break;
        case UPSERT:
          // We should not UPSERT record if same ID record exists, but with different natural key.
          if (recordCountFromId == recordCountFromNaturalKey) {
            collection.save(importRecord, WriteConcern.ACKNOWLEDGED);
            importedRecords++;
          }
          break;
        default:
          throw new IllegalArgumentException("Import mode " + mode + " is not supported");
      }
    }

    if (importedRecords + idClashCount + naturalKeyClashCount > 0) {
      log.info("{} out of {} '{}' records have been imported successfully in {} mode.", importedRecords, totalRecords,
          collectionName, mode);
      log.info("{} '{}' records have the same ID as existing records.", idClashCount, collectionName);
      log.info("{} '{}' records have the same natural key as existing records.", naturalKeyClashCount, collectionName);
      return ImportStatus.builder()
          .collectionName(collectionName)
          .imported(importedRecords)
          .idClashes(idClashCount)
          .naturalKeyClashes(naturalKeyClashCount)
          .build();
    } else {
      return null;
    }
  }

  private DBObject getNaturalKeyQueryFromKeyFields(String[] naturalKeyFields, DBObject importRecord) {
    // Check for if there is any existing record with the same natural key.
    List<BasicDBObject> objectList = new ArrayList<>();
    BasicDBObject lastQuery = null;
    for (String field : naturalKeyFields) {
      String fieldValue = (String) importRecord.get(field);
      if (fieldValue != null) {
        lastQuery = new BasicDBObject(field, fieldValue);
        objectList.add(lastQuery);
      }
    }

    if (objectList.size() > 1) {
      BasicDBObject andQuery = new BasicDBObject();
      andQuery.put("$and", objectList);
      return andQuery;
    } else {
      return lastQuery;
    }
  }

  private DBObject getDefaultNaturalKeyQuery(DBObject importRecord) {
    List<DBObject> filterList = new ArrayList<>();

    // Typical Mongo collection in Harness schema should have one subset of the following
    // combinations as their natural key.
    String name = (String) importRecord.get("name");
    if (name != null) {
      filterList.add(new BasicDBObject("name", name));
    }
    String email = (String) importRecord.get("email");
    if (email != null) {
      filterList.add(new BasicDBObject("email", email));
    }
    String hostname = (String) importRecord.get("hostname");
    if (hostname != null) {
      filterList.add(new BasicDBObject("hostname", hostname));
    }
    String publicDns = (String) importRecord.get("publicDns");
    if (publicDns != null) {
      filterList.add(new BasicDBObject("publicDns", publicDns));
    }
    String accountId = (String) importRecord.get("accountId");
    if (accountId != null) {
      filterList.add(new BasicDBObject("accountId", accountId));
    }
    String appId = (String) importRecord.get("appId");
    if (appId != null) {
      filterList.add(new BasicDBObject("appId", appId));
    }
    String envId = (String) importRecord.get("envId");
    if (envId != null) {
      filterList.add(new BasicDBObject("envId", envId));
    }
    String appName = (String) importRecord.get("appName");
    if (appName != null) {
      filterList.add(new BasicDBObject("appName", appName));
    }
    String artifactSourceName = (String) importRecord.get("artifactSourceName");
    if (artifactSourceName != null) {
      filterList.add(new BasicDBObject("artifactSourceName", artifactSourceName));
    }
    String displayName = (String) importRecord.get("displayName");
    if (displayName != null) {
      filterList.add(new BasicDBObject("displayName", displayName));
    }
    String folderId = (String) importRecord.get("folderId");
    if (folderId != null) {
      filterList.add(new BasicDBObject("folderId", folderId));
    }
    String artifactStreamId = (String) importRecord.get("artifactStreamId");
    if (artifactStreamId != null) {
      filterList.add(new BasicDBObject("artifactStreamId", artifactStreamId));
    }
    String templateId = (String) importRecord.get("templateId");
    if (templateId != null) {
      filterList.add(new BasicDBObject("templateId", templateId));
    }
    String galleryId = (String) importRecord.get("galleryId");
    if (galleryId != null) {
      filterList.add(new BasicDBObject("galleryId", galleryId));
    }
    Object version = importRecord.get("version");
    if (version != null) {
      filterList.add(new BasicDBObject("version", version));
    }
    String serviceId = (String) importRecord.get("serviceId");
    if (serviceId != null) {
      filterList.add(new BasicDBObject("serviceId", serviceId));
    }
    String encryptedDataId = (String) importRecord.get("encryptedDataId");
    if (encryptedDataId != null) {
      filterList.add(new BasicDBObject("encryptedDataId", encryptedDataId));
    }
    String infraMappingId = (String) importRecord.get("infraMappingId");
    if (infraMappingId != null) {
      filterList.add(new BasicDBObject("infraMappingId", infraMappingId));
    }

    // If non of the above field exists, fall back to use '_id' field as their natural key
    if (filterList.size() == 0) {
      Object id = importRecord.get("_id");
      if (id != null) {
        filterList.add(new BasicDBObject("_id", id));
      }
    }

    if (filterList.size() > 1) {
      BasicDBObject andQuery = new BasicDBObject();
      andQuery.put("$and", filterList);
      return andQuery;
    } else if (filterList.size() == 1) {
      return filterList.get(0);
    } else {
      return null;
    }
  }
}
