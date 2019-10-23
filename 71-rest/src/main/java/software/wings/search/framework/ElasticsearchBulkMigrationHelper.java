package software.wings.search.framework;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import software.wings.dl.WingsPersistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

@Slf4j
class ElasticsearchBulkMigrationHelper {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ElasticsearchClient elasticsearchClient;
  @Inject private ElasticsearchIndexManager elasticsearchIndexManager;
  private static final String BASE_CONFIGURATION_PATH = "/elasticsearch/framework/BaseViewSchema.json";
  private static final String ENTITY_CONFIGURATION_PATH_BASE = "/elasticsearch/entities/";

  private String getSearchConfiguration(String configurationPath) {
    try {
      String fullConfigurationPath = String.format("%s%s", ENTITY_CONFIGURATION_PATH_BASE, configurationPath);
      String entitySettingsString =
          IOUtils.toString(getClass().getResourceAsStream(fullConfigurationPath), StandardCharsets.UTF_8);
      String baseSettingsString =
          IOUtils.toString(getClass().getResourceAsStream(BASE_CONFIGURATION_PATH), StandardCharsets.UTF_8);
      return SearchEntityUtils.mergeSettings(baseSettingsString, entitySettingsString);
    } catch (IOException e) {
      logger.error("Failed to create index", e);
      return null;
    }
  }

  private boolean createIndex(
      SearchEntity<?> searchEntity, ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob) {
    String newConfiguration = getSearchConfiguration(searchEntity.getConfigurationPath());
    return elasticsearchIndexManager.createIndex(elasticsearchBulkMigrationJob.getNewIndexName(), newConfiguration);
  }

  private boolean insertDocument(String indexName, String entityId, String entityJson) {
    IndexRequest indexRequest = new IndexRequest(indexName);
    indexRequest.id(entityId);
    indexRequest.source(entityJson, XContentType.JSON);
    try {
      IndexResponse indexResponse = elasticsearchClient.index(indexRequest);
      return indexResponse.status() == RestStatus.OK || indexResponse.status() == RestStatus.CREATED;
    } catch (IOException e) {
      logger.error("Could not connect to elasticsearch", e);
    }
    return false;
  }

  private boolean upsertEntityBaseView(String indexName, EntityBaseView entityBaseView) {
    if (entityBaseView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(entityBaseView);
      if (!jsonString.isPresent()) {
        return false;
      }
      return insertDocument(indexName, entityBaseView.getId(), jsonString.get());
    }
    return true;
  }

  private <T extends PersistentEntity> boolean migrateDocuments(SearchEntity<T> searchEntity, String newIndexName) {
    Class<T> sourceEntityClass = searchEntity.getSourceEntityClass();
    try (HIterator<T> iterator = new HIterator<>(wingsPersistence.createQuery(sourceEntityClass).fetch())) {
      while (iterator.hasNext()) {
        final T object = iterator.next();
        EntityBaseView entityBaseView = searchEntity.getView(object);
        if (!upsertEntityBaseView(newIndexName, entityBaseView)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean reconfigureAlias(ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob, String type) {
    String aliasName = elasticsearchIndexManager.getAliasName(type);
    boolean isIndexAttached =
        elasticsearchIndexManager.attachIndexToAlias(aliasName, elasticsearchBulkMigrationJob.getNewIndexName());
    String oldIndexName = elasticsearchBulkMigrationJob.getOldIndexName();
    if (isIndexAttached && oldIndexName != null) {
      return elasticsearchIndexManager.removeIndexFromAlias(oldIndexName);
    }
    return isIndexAttached;
  }

  private boolean runBulkMigration(SearchEntity<?> searchEntity) {
    ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob =
        wingsPersistence.get(ElasticsearchBulkMigrationJob.class, searchEntity.getClass().getCanonicalName());
    boolean isMigrationFailed = !createIndex(searchEntity, elasticsearchBulkMigrationJob);

    if (!isMigrationFailed) {
      isMigrationFailed = !migrateDocuments(searchEntity, elasticsearchBulkMigrationJob.getNewIndexName());
    }

    return !isMigrationFailed;
  }

  private boolean runPostMigrationStep(SearchEntity<?> searchEntity) {
    ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob =
        wingsPersistence.get(ElasticsearchBulkMigrationJob.class, searchEntity.getClass().getCanonicalName());
    return reconfigureAlias(elasticsearchBulkMigrationJob, searchEntity.getType());
  }

  boolean doBulkSync(Set<SearchEntity<?>> entitiesToBulkSync) {
    boolean hasMigrationSucceeded = true;
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
      for (SearchEntity<?> searchEntity : entitiesToBulkSync) {
        hasMigrationSucceeded = hasMigrationSucceeded && runPostMigrationStep(searchEntity);
      }
    }

    return hasMigrationSucceeded;
  }
}
