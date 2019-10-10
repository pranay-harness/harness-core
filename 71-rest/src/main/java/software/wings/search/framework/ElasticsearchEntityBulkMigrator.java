package software.wings.search.framework;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.indices.CreateIndexResponse;
import software.wings.dl.WingsPersistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
class ElasticsearchEntityBulkMigrator {
  @Inject private SearchDao searchDao;
  @Inject private ElasticsearchIndexManager elasticsearchIndexManager;
  @Inject private WingsPersistence wingsPersistence;
  private static final String BASE_CONFIGURATION_PATH = "/elasticsearch/framework/BaseViewSchema.json";
  private static final String ENTITY_CONFIGURATION_PATH_BASE = "/elasticsearch/entities/";

  private boolean deleteIndex(SearchEntity<?> searchEntity) {
    try {
      boolean exists = elasticsearchIndexManager.isIndexPresent(searchEntity.getType());
      if (exists) {
        logger.info(String.format("%s index exists. Deleting the index", searchEntity.getType()));
        AcknowledgedResponse deleteIndexResponse = elasticsearchIndexManager.deleteIndex(searchEntity.getType());
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

  private String getSearchConfiguration(SearchEntity<?> searchEntity) throws IOException {
    String configurationPath =
        String.format("%s%s", ENTITY_CONFIGURATION_PATH_BASE, searchEntity.getConfigurationPath());
    String entitySettingsString =
        IOUtils.toString(getClass().getResourceAsStream(configurationPath), StandardCharsets.UTF_8);
    String baseSettingsString =
        IOUtils.toString(getClass().getResourceAsStream(BASE_CONFIGURATION_PATH), StandardCharsets.UTF_8);
    return SearchEntityUtils.mergeSettings(baseSettingsString, entitySettingsString);
  }

  private boolean createIndex(SearchEntity<?> searchEntity) {
    try {
      String entityConfiguration = getSearchConfiguration(searchEntity);
      CreateIndexResponse createIndexResponse =
          elasticsearchIndexManager.createIndex(searchEntity.getType(), entityConfiguration);
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

  private boolean upsertEntityBaseView(SearchEntity searchEntity, EntityBaseView entityBaseView) {
    if (entityBaseView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(entityBaseView);
      if (!jsonString.isPresent()) {
        return false;
      }
      return searchDao.insertDocument(searchEntity.getType(), entityBaseView.getId(), jsonString.get());
    }
    return true;
  }

  <T extends PersistentEntity> boolean runBulkMigration(SearchEntity<T> searchEntity) {
    boolean isIndexRecreated = recreate(searchEntity);
    if (!isIndexRecreated) {
      return false;
    }

    Class<T> sourceEntityClass = searchEntity.getSourceEntityClass();
    try (HIterator<T> iterator = new HIterator<>(wingsPersistence.createQuery(sourceEntityClass).fetch())) {
      while (iterator.hasNext()) {
        final T object = iterator.next();
        EntityBaseView entityBaseView = searchEntity.getView(object);
        if (!upsertEntityBaseView(searchEntity, entityBaseView)) {
          return false;
        }
      }
    }
    return true;
  }
}
