/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeSubscriber;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.search.framework.ElasticsearchBulkMigrationJob.ElasticsearchBulkMigrationJobBuilder;
import software.wings.search.framework.SearchEntityIndexState.SearchEntityIndexStateKeys;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * The task responsible for carrying out the bulk sync
 * from the persistence layer to elasticsearch.
 *
 * @author utkarsh
 */

@OwnedBy(PL)
@Slf4j
public class ElasticsearchBulkSyncTask {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ElasticsearchSyncHelper elasticsearchSyncHelper;
  @Inject private ElasticsearchBulkMigrationHelper elasticsearchBulkMigrationHelper;
  @Inject private ElasticsearchIndexManager elasticsearchIndexManager;
  @Inject private Set<SearchEntity<?>> searchEntities;
  private Queue<ChangeEvent<?>> changeEventsDuringBulkSync;
  private Map<Class, Boolean> isFirstChangeReceived;

  private void cleanupFailedBulkMigrationJobs() {
    try (HIterator<ElasticsearchBulkMigrationJob> iterator =
             new HIterator<>(wingsPersistence.createQuery(ElasticsearchBulkMigrationJob.class).fetch())) {
      while (iterator.hasNext()) {
        ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob = iterator.next();
        String indexName = elasticsearchBulkMigrationJob.getNewIndexName();
        elasticsearchIndexManager.deleteIndex(indexName);
        wingsPersistence.delete(ElasticsearchBulkMigrationJob.class, elasticsearchBulkMigrationJob.getEntityClass());
      }
    }
  }

  private boolean updateSearchEntityIndexState(ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob) {
    Query<SearchEntityIndexState> query = wingsPersistence.createQuery(SearchEntityIndexState.class)
                                              .field(SearchEntityIndexStateKeys.entityClass)
                                              .equal(elasticsearchBulkMigrationJob.getEntityClass());

    UpdateOperations<SearchEntityIndexState> updateOperations =
        wingsPersistence.createUpdateOperations(SearchEntityIndexState.class)
            .set(SearchEntityIndexStateKeys.indexName, elasticsearchBulkMigrationJob.getNewIndexName())
            .set(SearchEntityIndexStateKeys.syncVersion, elasticsearchBulkMigrationJob.getToVersion())
            .set(SearchEntityIndexStateKeys.recreateIndex, false);

    SearchEntityIndexState searchEntityIndexState =
        wingsPersistence.upsert(query, updateOperations, upsertReturnNewOptions);
    if (searchEntityIndexState == null
        || !searchEntityIndexState.getIndexName().equals(elasticsearchBulkMigrationJob.getNewIndexName())) {
      log.error("Search entitiy {} index state did not update", elasticsearchBulkMigrationJob.getEntityClass());
      return false;
    }
    return true;
  }

  private boolean cleanupSuccessfulBulkMigrationJobs(Set<SearchEntity<?>> syncedBulkEntites) {
    boolean isRunningSuccessfully = true;
    Iterator<SearchEntity<?>> iterator = syncedBulkEntites.iterator();
    while (iterator.hasNext() && isRunningSuccessfully) {
      SearchEntity<?> syncedBulkEntity = iterator.next();
      ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob =
          wingsPersistence.get(ElasticsearchBulkMigrationJob.class, syncedBulkEntity.getClass().getCanonicalName());
      isRunningSuccessfully = updateSearchEntityIndexState(elasticsearchBulkMigrationJob)
          && wingsPersistence.delete(
              ElasticsearchBulkMigrationJob.class, syncedBulkEntity.getClass().getCanonicalName());
    }
    return isRunningSuccessfully;
  }

  private Set<SearchEntity<?>> getEntitiesToBulkSync(Collection<SearchEntity<?>> searchEntityList) {
    Set<SearchEntity<?>> entitiesToBulkSync = new HashSet<>();
    for (SearchEntity<?> searchEntity : searchEntityList) {
      SearchEntityIndexState searchEntityIndexState =
          wingsPersistence.get(SearchEntityIndexState.class, searchEntity.getClass().getCanonicalName());
      if (searchEntityIndexState != null && !searchEntityIndexState.shouldBulkSync()) {
        log.info("Entity {} is already migrated to elasticsearch", searchEntity.getClass());
      } else {
        log.info("Entity {} is to be migrated to elasticsearch", searchEntity.getClass());
        entitiesToBulkSync.add(searchEntity);
      }
    }
    return entitiesToBulkSync;
  }

  private String generateIndexName(String type, String version) {
    long timestamp = Instant.now().toEpochMilli();
    return type.concat("_").concat(version).concat("_").concat(Long.toString(timestamp));
  }

  private SearchEntityIndexState getSearchEntityIndexState(SearchEntity<?> searchEntity) {
    return wingsPersistence.get(SearchEntityIndexState.class, searchEntity.getClass().getCanonicalName());
  }

  private boolean createSearchBulkMigrationJob(SearchEntityIndexState searchEntityIndexState, String newIndexName,
      String newVersion, Class<? extends SearchEntity> searchEntityClass) {
    ElasticsearchBulkMigrationJobBuilder elasticsearchBulkMigrationJobBuilder =
        ElasticsearchBulkMigrationJob.builder()
            .entityClass(searchEntityClass.getCanonicalName())
            .newIndexName(newIndexName)
            .toVersion(newVersion);
    if (searchEntityIndexState != null) {
      elasticsearchBulkMigrationJobBuilder.oldIndexName(searchEntityIndexState.getIndexName())
          .fromVersion(searchEntityIndexState.getSyncVersion());
    }
    ElasticsearchBulkMigrationJob elasticsearchBulkMigrationJob = elasticsearchBulkMigrationJobBuilder.build();
    return wingsPersistence.save(elasticsearchBulkMigrationJob) != null;
  }

  private boolean createElasticsearchBulkMigrationJobs(Set<SearchEntity<?>> entitiesToBulkSync) {
    boolean areJobsCreated = true;
    for (SearchEntity<?> searchEntity : entitiesToBulkSync) {
      SearchEntityIndexState searchEntityIndexState = getSearchEntityIndexState(searchEntity);
      String newIndexName = generateIndexName(searchEntity.getType(), searchEntity.getVersion());
      areJobsCreated = areJobsCreated
          && createSearchBulkMigrationJob(
              searchEntityIndexState, newIndexName, searchEntity.getVersion(), searchEntity.getClass());
    }
    return areJobsCreated;
  }

  private ChangeSubscriber<?> getChangeSubscriber() {
    return changeEvent -> {
      if (isFirstChangeReceived.get(changeEvent.getEntityType()) == null) {
        changeEventsDuringBulkSync.add(changeEvent);
        isFirstChangeReceived.put(changeEvent.getEntityType(), true);
      }
    };
  }

  public ElasticsearchBulkSyncTaskResult run() {
    changeEventsDuringBulkSync = new LinkedList<>();
    isFirstChangeReceived = new HashMap<>();

    log.info("Clean up failed migration jobs");
    cleanupFailedBulkMigrationJobs();

    log.info("Initializing change listeners for search entities for bulk sync.");
    elasticsearchSyncHelper.startChangeListeners(getChangeSubscriber());

    log.info("Getting the entities that have to bulk synced");
    Set<SearchEntity<?>> entitiesToBulkSync = getEntitiesToBulkSync(searchEntities);

    log.info("Create jobs for bulk migration of search entities");
    boolean areJobsCreated = createElasticsearchBulkMigrationJobs(entitiesToBulkSync);

    if (!areJobsCreated) {
      return new ElasticsearchBulkSyncTaskResult(false, changeEventsDuringBulkSync);
    }

    log.info("Starting migration of entities from persistence to search database");
    boolean hasMigrationSucceeded = elasticsearchBulkMigrationHelper.doBulkSync(entitiesToBulkSync);

    log.info("Calling change tracker to close change listeners after bulk sync was completed");
    elasticsearchSyncHelper.stopChangeListeners();

    if (hasMigrationSucceeded) {
      hasMigrationSucceeded = cleanupSuccessfulBulkMigrationJobs(entitiesToBulkSync);
      return new ElasticsearchBulkSyncTaskResult(hasMigrationSucceeded, changeEventsDuringBulkSync);
    }

    return new ElasticsearchBulkSyncTaskResult(false, changeEventsDuringBulkSync);
  }
}
