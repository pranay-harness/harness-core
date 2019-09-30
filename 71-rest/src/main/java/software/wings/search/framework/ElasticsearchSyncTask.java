package software.wings.search.framework;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.SearchEntityVersion.SearchEntityVersionKeys;
import software.wings.search.framework.SearchSourceEntitySyncState.SearchSourceEntitySyncStateKeys;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeSubscriber;
import software.wings.search.framework.changestreams.ChangeTracker;
import software.wings.search.framework.changestreams.ChangeTrackingInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Abstract template class for both
 * realtime and bulk synchronisation tasks.
 *
 * @author utkarsh
 */

@Slf4j
public abstract class ElasticsearchSyncTask {
  @Inject protected Map<Class<? extends PersistentEntity>, SearchEntity<?>> searchEntityMap;
  @Inject protected WingsPersistence wingsPersistence;
  @Inject private ChangeTracker changeTracker;

  private boolean saveSearchSourceEntitySyncStateToken(Class<? extends PersistentEntity> sourceClass, String token) {
    String sourceClassName = sourceClass.getCanonicalName();

    Query<SearchSourceEntitySyncState> query = wingsPersistence.createQuery(SearchSourceEntitySyncState.class)
                                                   .field(SearchSourceEntitySyncStateKeys.sourceEntityClass)
                                                   .equal(sourceClassName);

    UpdateOperations<SearchSourceEntitySyncState> updateOperations =
        wingsPersistence.createUpdateOperations(SearchSourceEntitySyncState.class)
            .set(SearchSourceEntitySyncStateKeys.lastSyncedToken, token);

    SearchSourceEntitySyncState searchSourceEntitySyncState =
        wingsPersistence.upsert(query, updateOperations, HPersistence.upsertReturnNewOptions);
    if (searchSourceEntitySyncState == null || !searchSourceEntitySyncState.getLastSyncedToken().equals(token)) {
      logger.error(
          String.format("Search Entity %s token %s could not be updated", sourceClass.getCanonicalName(), token));
      return false;
    }
    return true;
  }

  protected boolean updateSearchEntitySyncStateVersion(SearchEntity<?> searchEntity) {
    String searchEntityClassName = searchEntity.getClass().getCanonicalName();

    Query<SearchEntityVersion> query = wingsPersistence.createQuery(SearchEntityVersion.class)
                                           .field(SearchEntityVersionKeys.entityClass)
                                           .equal(searchEntityClassName);

    UpdateOperations<SearchEntityVersion> updateOperations =
        wingsPersistence.createUpdateOperations(SearchEntityVersion.class)
            .set(SearchEntityVersionKeys.syncVersion, searchEntity.getVersion());

    SearchEntityVersion searchEntityVersion =
        wingsPersistence.upsert(query, updateOperations, HPersistence.upsertReturnNewOptions);
    if (searchEntityVersion == null || !searchEntityVersion.getSyncVersion().equals(searchEntity.getVersion())) {
      logger.error(
          String.format("Search Entity %s version could not be updated", searchEntity.getClass().getCanonicalName()));
      return false;
    }
    return true;
  }

  protected Future<?> initializeChangeListeners() {
    Set<Class<? extends PersistentEntity>> subscribedClasses = new HashSet<>();

    searchEntityMap.values().forEach(searchEntity -> subscribedClasses.addAll(searchEntity.getSubscriptionEntities()));

    Set<ChangeTrackingInfo<?>> changeTrackingInfos = new HashSet<>();

    for (Class<? extends PersistentEntity> subscribedClass : subscribedClasses) {
      ChangeTrackingInfo<?> changeTrackingInfo = getChangeTrackingInfo(subscribedClass);
      changeTrackingInfos.add(changeTrackingInfo);
    }

    logger.info("Calling change tracker to start change listeners");
    return changeTracker.start(changeTrackingInfos);
  }

  private <T extends PersistentEntity> ChangeTrackingInfo<T> getChangeTrackingInfo(Class<T> subscribedClass) {
    SearchSourceEntitySyncState searchSourceEntitySyncState =
        wingsPersistence.get(SearchSourceEntitySyncState.class, subscribedClass.getCanonicalName());
    String token = null;
    if (searchSourceEntitySyncState != null) {
      token = searchSourceEntitySyncState.getLastSyncedToken();
    }
    ChangeSubscriber<T> changeSubscriber = getChangeSubscriber();
    return new ChangeTrackingInfo<>(subscribedClass, changeSubscriber, token);
  }

  protected void stopChangeListeners() {
    logger.info("Stopping change listeners");
    changeTracker.stop();
  }

  protected boolean processChange(ChangeEvent<?> changeEvent) {
    Class<? extends PersistentEntity> sourceClass = changeEvent.getEntityType();
    for (SearchEntity<?> searchEntity : searchEntityMap.values()) {
      if (searchEntity.getSubscriptionEntities().contains(sourceClass)) {
        ChangeHandler changeHandler = searchEntity.getChangeHandler();
        boolean isChangeHandled = changeHandler.handleChange(changeEvent);
        if (!isChangeHandled) {
          logger.error(String.format("ChangeEvent %s could not be processed for entity %s", changeEvent.toString(),
              searchEntity.getClass().getCanonicalName()));
          return false;
        }
      }
    }
    boolean isSaved = saveSearchSourceEntitySyncStateToken(sourceClass, changeEvent.getToken());
    if (!isSaved) {
      logger.error(String.format("ChangeEvent %s could not be processed for entity %s", changeEvent.toString(),
          sourceClass.getCanonicalName()));
    }
    return isSaved;
  }

  private <T extends PersistentEntity> ChangeSubscriber<T> getChangeSubscriber() {
    return this ::onChange;
  }

  public abstract boolean run();

  protected abstract void onChange(ChangeEvent<?> changeEvent);
}
