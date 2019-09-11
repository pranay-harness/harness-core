package software.wings.search.framework;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import software.wings.search.framework.changestreams.ChangeEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * The task responsible for carrying out the bulk sync
 * from the persistence layer to elasticsearch.
 *
 * @author utkarsh
 */

@Slf4j
public class ElasticsearchBulkSyncTask extends ElasticsearchSyncTask {
  @Inject RestHighLevelClient client;
  private Queue<ChangeEvent> changeEventsDuringBulkSync = new LinkedList<>();
  private Map<Class, Boolean> isFirstChangeReceived = new HashMap<>();
  private Set<SearchEntity<?>> entitiesToBulkSync = new HashSet<>();
  private static final String BASE_CONFIGURATION_PATH = "/elasticsearch/framework/BaseViewSchema.json";
  private static final String ENTITY_CONFIGURATION_PATH_BASE = "/elasticsearch/entities/";

  private void setEntitiesToBulkSync() {
    for (SearchEntity<?> searchEntity : searchEntityMap.values()) {
      String searchEntityClassName = searchEntity.getClass().getCanonicalName();
      SearchEntitySyncState searchEntitySyncState =
          wingsPersistence.get(SearchEntitySyncState.class, searchEntityClassName);
      if (searchEntitySyncState != null && !searchEntitySyncState.shouldBulkSync()) {
        logger.info(String.format("Entity %s is already migrated to elasticsearch", searchEntityClassName));
      } else {
        logger.info(String.format("Entity %s is to be migrated to elasticsearch", searchEntityClassName));
        entitiesToBulkSync.add(searchEntity);
      }
    }
  }

  private boolean deleteIndex(SearchEntity<?> searchEntity) {
    try {
      String indexName = ElasticsearchUtils.getIndexName(searchEntity.getType());
      GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
      boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
      if (exists) {
        logger.info(String.format("%s index exists. Deleting the index", searchEntity.getType()));
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);

        AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        if (deleteIndexResponse == null || !deleteIndexResponse.isAcknowledged()) {
          logger.error(
              String.format("Could not delete index for searchEntity %s", searchEntity.getClass().getCanonicalName()));
          return false;
        }
      }
    } catch (IOException e) {
      logger.error("Failed to delete index", e);
      return false;
    }
    return true;
  }

  public String getSearchConfiguration(SearchEntity<?> searchEntity) throws IOException {
    String CONFIGURATION_PATH =
        String.format("%s%s", ENTITY_CONFIGURATION_PATH_BASE, searchEntity.getConfigurationPath());
    String entitySettingsString =
        IOUtils.toString(getClass().getResourceAsStream(CONFIGURATION_PATH), StandardCharsets.UTF_8);
    String baseSettingsString =
        IOUtils.toString(getClass().getResourceAsStream(BASE_CONFIGURATION_PATH), StandardCharsets.UTF_8);
    return SearchEntityUtils.mergeSettings(baseSettingsString, entitySettingsString);
  }

  private boolean createIndex(SearchEntity<?> searchEntity) {
    try {
      String indexName = ElasticsearchUtils.getIndexName(searchEntity.getType());
      CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);

      String entityConfiguration = getSearchConfiguration(searchEntity);
      createIndexRequest.source(entityConfiguration, XContentType.JSON);

      CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
      if (createIndexResponse == null || !createIndexResponse.isAcknowledged()) {
        logger.error(
            String.format("Could not create index for searchEntity %s", searchEntity.getClass().getCanonicalName()));
        return false;
      }

    } catch (IOException e) {
      logger.error("Failed to create index", e);
      return false;
    }
    return true;
  }

  private boolean recreate(SearchEntity<?> searchEntity) {
    boolean isIndexDeleted = deleteIndex(searchEntity);
    if (!isIndexDeleted) {
      return false;
    }
    return createIndex(searchEntity);
  }

  private <T extends PersistentEntity> boolean runBulkMigration(SearchEntity<T> searchEntity) {
    boolean isIndexRecreated = recreate(searchEntity);
    if (!isIndexRecreated) {
      return false;
    }

    Class<T> sourceEntityClass = searchEntity.getSourceEntityClass();
    try (HIterator<T> iterator = new HIterator<>(wingsPersistence.createQuery(sourceEntityClass).fetch())) {
      while (iterator.hasNext()) {
        final T object = iterator.next();
        EntityBaseView entityBaseView = searchEntity.getView(object);
        Optional<String> jsonString = SearchEntityUtils.convertToJson(entityBaseView);
        if (!jsonString.isPresent()) {
          return false;
        }
        boolean isUpserted =
            ElasticsearchUtils.upsertDocument(client, searchEntity.getType(), entityBaseView.getId(), jsonString.get());
        if (!isUpserted) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean processChanges(Queue<ChangeEvent> changeEvents) {
    while (!changeEvents.isEmpty()) {
      ChangeEvent changeEvent = changeEvents.poll();
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
    logger.info("Initializing change listeners for search entities");
    super.initializeChangeListeners();

    logger.info("Getting the entities that have to bulk synced");
    setEntitiesToBulkSync();

    logger.info("Starting migration of entities from persistence to search database");

    boolean hasMigrationSucceeded = entitiesToBulkSync.isEmpty();

    for (SearchEntity<?> searchEntity : entitiesToBulkSync) {
      logger.info(String.format("Migrating %s to elasticsearch", searchEntity.getClass().getCanonicalName()));
      hasMigrationSucceeded = runBulkMigration(searchEntity);
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

    logger.info("Calling change tracker to close change listeners");
    super.stopChangeListeners();

    return hasMigrationSucceeded;
  }

  public void handleChangeEvent(ChangeEvent changeEvent) {
    if (isFirstChangeReceived.get(changeEvent.getEntityType()) == null) {
      changeEventsDuringBulkSync.add(changeEvent);
      isFirstChangeReceived.put(changeEvent.getEntityType(), true);
    }
  }
}
