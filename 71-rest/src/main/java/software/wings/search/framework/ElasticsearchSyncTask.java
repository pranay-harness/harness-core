package software.wings.search.framework;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.SearchEntitySyncState.SearchEntitySyncStateKeys;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeTracker;
import software.wings.search.framework.changestreams.ChangeTrackingInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

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
  protected CountDownLatch latch;

  protected boolean saveSearchEntitySyncStateToken(SearchEntity<?> searchEntity, String token) {
    String searchEntityClassName = searchEntity.getClass().getCanonicalName();

    Query<SearchEntitySyncState> query = wingsPersistence.createQuery(SearchEntitySyncState.class)
                                             .field(SearchEntitySyncStateKeys.searchEntityClass)
                                             .equal(searchEntityClassName);

    UpdateOperations<SearchEntitySyncState> updateOperations =
        wingsPersistence.createUpdateOperations(SearchEntitySyncState.class)
            .set(SearchEntitySyncStateKeys.lastSyncedToken, token);

    SearchEntitySyncState searchEntitySyncState =
        wingsPersistence.upsert(query, updateOperations, HPersistence.upsertReturnNewOptions);
    if (searchEntitySyncState == null || !searchEntitySyncState.getLastSyncedToken().equals(token)) {
      logger.error(String.format(
          "Search Entity %s token %s could not be updated", searchEntity.getClass().getCanonicalName(), token));
      return false;
    }
    return true;
  }

  protected boolean updateSearchEntitySyncStateVersion(SearchEntity<?> searchEntity) {
    String searchEntityClassName = searchEntity.getClass().getCanonicalName();

    Query<SearchEntitySyncState> query = wingsPersistence.createQuery(SearchEntitySyncState.class)
                                             .field(SearchEntitySyncStateKeys.searchEntityClass)
                                             .equal(searchEntityClassName);

    UpdateOperations<SearchEntitySyncState> updateOperations =
        wingsPersistence.createUpdateOperations(SearchEntitySyncState.class)
            .set(SearchEntitySyncStateKeys.syncVersion, searchEntity.getVersion());

    SearchEntitySyncState searchEntitySyncState =
        wingsPersistence.upsert(query, updateOperations, HPersistence.upsertReturnNewOptions);
    if (searchEntitySyncState == null || !searchEntitySyncState.getSyncVersion().equals(searchEntity.getVersion())) {
      logger.error(
          String.format("Search Entity %s version could not be updated", searchEntity.getClass().getCanonicalName()));
      return false;
    }
    return true;
  }

  protected void initializeChangeListeners() {
    latch = new CountDownLatch(searchEntityMap.size());

    Set<ChangeTrackingInfo> changeTrackingInfos = new HashSet<>();
    for (SearchEntity<?> searchEntity : searchEntityMap.values()) {
      String searchEntityClassName = searchEntity.getClass().getCanonicalName();
      Class<? extends PersistentEntity> sourceEntity = searchEntity.getSourceEntityClass();
      SearchEntitySyncState searchEntitySyncState =
          wingsPersistence.get(SearchEntitySyncState.class, searchEntityClassName);
      String token = null;
      if (searchEntitySyncState != null) {
        token = searchEntitySyncState.getLastSyncedToken();
      }
      ChangeTrackingInfo changeTrackingInfo = new ChangeTrackingInfo(sourceEntity, changeEventConsumer, token);
      changeTrackingInfos.add(changeTrackingInfo);
    }

    logger.info("Calling change tracker to start change listeners");
    changeTracker.start(changeTrackingInfos, latch);
  }

  protected void stopChangeListeners() {
    logger.info("Stopping change listeners");
    changeTracker.stop();
  }

  protected boolean processChange(ChangeEvent changeEvent) {
    for (SearchEntity<?> searchEntity : searchEntityMap.values()) {
      ChangeHandler changeHandler = searchEntity.getChangeHandler();
      boolean isChangeHandled = changeHandler.handleChange(changeEvent);

      if (!isChangeHandled) {
        logger.error(String.format("ChangeEvent %s could not be processed for entity %s", changeEvent.toString(),
            searchEntity.getClass().getCanonicalName()));
        return false;
      }
    }
    SearchEntity<?> searchEntity = searchEntityMap.get(changeEvent.getEntityType());
    boolean isSaved = saveSearchEntitySyncStateToken(searchEntity, changeEvent.getToken());
    if (!isSaved) {
      logger.error(String.format("ChangeEvent %s could not be processed for entity %s", changeEvent.toString(),
          searchEntity.getClass().getCanonicalName()));
    }
    return isSaved;
  }

  protected Consumer<ChangeEvent> changeEventConsumer = new Consumer<ChangeEvent>() {
    @Override
    public synchronized void accept(ChangeEvent changeEvent) {
      handleChangeEvent(changeEvent);
    }
  };

  public abstract boolean run();

  protected abstract void handleChangeEvent(ChangeEvent changeEvent);
}
