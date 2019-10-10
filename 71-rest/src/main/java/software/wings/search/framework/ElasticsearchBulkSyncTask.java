package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.search.framework.changestreams.ChangeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * The task responsible for carrying out the bulk sync
 * from the persistence layer to elasticsearch.
 *
 * @author utkarsh
 */

@Slf4j
public class ElasticsearchBulkSyncTask extends ElasticsearchSyncTask {
  @Inject ElasticsearchEntityBulkMigrator elasticsearchEntityBulkMigrator;
  private Queue<ChangeEvent<?>> changeEventsDuringBulkSync = new LinkedList<>();
  private Map<Class, Boolean> isFirstChangeReceived = new HashMap<>();
  private Set<SearchEntity<?>> entitiesToBulkSync = new HashSet<>();

  private void setEntitiesToBulkSync() {
    for (SearchEntity<?> searchEntity : searchEntityMap.values()) {
      SearchEntityVersion searchEntityVersion =
          wingsPersistence.get(SearchEntityVersion.class, searchEntity.getClass().getCanonicalName());
      if (searchEntityVersion != null && !searchEntityVersion.shouldBulkSync()) {
        logger.info(String.format("Entity %s is already migrated to elasticsearch", searchEntity.getClass()));
      } else {
        logger.info(String.format("Entity %s is to be migrated to elasticsearch", searchEntity.getClass()));
        entitiesToBulkSync.add(searchEntity);
      }
    }
  }

  private boolean processChanges(Queue<ChangeEvent<?>> changeEvents) {
    while (!changeEvents.isEmpty()) {
      ChangeEvent<?> changeEvent = changeEvents.poll();
      boolean isChangeProcessed = super.processChange(changeEvent);
      if (!isChangeProcessed) {
        return false;
      }
    }
    return true;
  }

  private boolean updateVersions(Set<SearchEntity<?>> searchEntities) {
    for (SearchEntity<?> searchEntity : searchEntities) {
      boolean isChangeProcessed = super.updateSearchEntitySyncStateVersion(searchEntity);
      if (!isChangeProcessed) {
        return false;
      }
    }
    return true;
  }

  public boolean run() {
    logger.info("Initializing change listeners for search entities for bulk sync.");
    Future f = super.initializeChangeListeners();

    logger.info("Getting the entities that have to bulk synced");
    setEntitiesToBulkSync();

    logger.info("Starting migration of entities from persistence to search database");

    boolean hasMigrationSucceeded = entitiesToBulkSync.isEmpty();

    for (SearchEntity<?> searchEntity : entitiesToBulkSync) {
      logger.info(String.format("Migrating %s to elasticsearch", searchEntity.getClass().getCanonicalName()));
      hasMigrationSucceeded = elasticsearchEntityBulkMigrator.runBulkMigration(searchEntity);
      if (hasMigrationSucceeded) {
        logger.info(String.format("%s migrated to elasticsearch", searchEntity.getClass().getCanonicalName()));
      } else {
        logger.error(
            String.format("Failed to migrate %s to elasticsearch", searchEntity.getClass().getCanonicalName()));
        break;
      }
    }

    if (hasMigrationSucceeded) {
      logger.info("Processing changes received during bulk migration");
      boolean areChangesProcessed = processChanges(changeEventsDuringBulkSync);
      boolean areVersionsUpdated = false;

      if (areChangesProcessed) {
        logger.info("Bulk migration successful. Updating search entities version to persistence layer");
        areVersionsUpdated = updateVersions(entitiesToBulkSync);
      }

      hasMigrationSucceeded = areChangesProcessed && areVersionsUpdated;
    }

    logger.info("Calling change tracker to close change listeners after bulk sync was completed.");
    super.stopChangeListeners();
    f.cancel(true);

    return hasMigrationSucceeded;
  }

  public void onChange(ChangeEvent<?> changeEvent) {
    if (isFirstChangeReceived.get(changeEvent.getEntityType()) == null) {
      changeEventsDuringBulkSync.add(changeEvent);
      isFirstChangeReceived.put(changeEvent.getEntityType(), true);
    }
  }
}
