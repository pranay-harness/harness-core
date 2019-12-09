package software.wings.search.framework;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import software.wings.dl.WingsPersistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
class ElasticsearchBulkMigrationHelper {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ElasticsearchClient elasticsearchClient;
  @Inject private ElasticsearchIndexManager elasticsearchIndexManager;
  private static final int NUMBER_OF_BULK_SYNC_THREADS = 5;
  private static final int BULK_SYNC_TASK_QUEUE_SIZE = 200;
  private static final String BULK_THREAD_SUFFIX = "-bulk-migration-%d";
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

  private <T extends PersistentEntity> boolean migrateDocuments(SearchEntity<T> searchEntity, String newIndexName) {
    String threadPoolNameFormat = searchEntity.getClass().getSimpleName().concat(BULK_THREAD_SUFFIX);
    ExecutorService executorService = Executors.newFixedThreadPool(
        NUMBER_OF_BULK_SYNC_THREADS, new ThreadFactoryBuilder().setNameFormat(threadPoolNameFormat).build());
    BoundedExecutorService boundedExecutorService =
        new BoundedExecutorService(executorService, BULK_SYNC_TASK_QUEUE_SIZE);
    List<Future<Boolean>> taskFutures = new ArrayList<>();

    Class<T> sourceEntityClass = searchEntity.getSourceEntityClass();

    try (HIterator<T> iterator = new HIterator<>(wingsPersistence.createQuery(sourceEntityClass).fetch())) {
      while (iterator.hasNext()) {
        final T object = iterator.next();
        SearchEntityBulkMigrationTask<T> searchEntityBulkMigrationTask =
            new SearchEntityBulkMigrationTask<>(elasticsearchClient, searchEntity, object, newIndexName);
        taskFutures.add(boundedExecutorService.submit(searchEntityBulkMigrationTask));
      }
      for (Future<Boolean> taskFuture : taskFutures) {
        boolean result = taskFuture.get();
        if (!result) {
          return false;
        }
      }
      return true;
    } catch (InterruptedException e) {
      logger.error("Bulk migration interrupted", e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      logger.error("Bulk migration errored due to error", e.getCause());
    } finally {
      executorService.shutdownNow();
    }
    return false;
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
      logger.info("Migrating {} to elasticsearch", searchEntity.getClass().getCanonicalName());
      hasMigrationSucceeded = runBulkMigration(searchEntity);

      if (hasMigrationSucceeded) {
        logger.info("{} migrated to elasticsearch", searchEntity.getClass().getCanonicalName());
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
