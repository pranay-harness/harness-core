package software.wings.search.framework;

import com.google.inject.Inject;

import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.changestreams.ChangeSubscriber;
import software.wings.search.framework.changestreams.ChangeTracker;
import software.wings.search.framework.changestreams.ChangeTrackingInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract template class for both
 * realtime and bulk synchronisation tasks.
 *
 * @author utkarsh
 */

@Slf4j
class ElasticsearchSyncHelper {
  @Inject private Set<SearchEntity<?>> searchEntities;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ChangeTracker changeTracker;

  void startChangeListeners(ChangeSubscriber changeSubscriber) {
    Set<Class<? extends PersistentEntity>> subscribedClasses = new HashSet<>();
    searchEntities.forEach(searchEntity -> subscribedClasses.addAll(searchEntity.getSubscriptionEntities()));

    Set<ChangeTrackingInfo<?>> changeTrackingInfos = new HashSet<>();

    for (Class<? extends PersistentEntity> subscribedClass : subscribedClasses) {
      ChangeTrackingInfo<?> changeTrackingInfo = getChangeTrackingInfo(subscribedClass, changeSubscriber);
      changeTrackingInfos.add(changeTrackingInfo);
    }

    logger.info("Calling change tracker to start change listeners");
    changeTracker.start(changeTrackingInfos);
  }

  private <T extends PersistentEntity> ChangeTrackingInfo<T> getChangeTrackingInfo(
      Class<T> subscribedClass, ChangeSubscriber<T> changeSubscriber) {
    SearchSourceEntitySyncState searchSourceEntitySyncState =
        wingsPersistence.get(SearchSourceEntitySyncState.class, subscribedClass.getCanonicalName());
    String token = null;
    if (searchSourceEntitySyncState != null) {
      token = searchSourceEntitySyncState.getLastSyncedToken();
    }
    return new ChangeTrackingInfo<>(subscribedClass, changeSubscriber, token);
  }

  boolean checkIfAnyChangeListenerIsAlive() {
    return changeTracker.checkIfAnyChangeTrackerIsAlive();
  }

  void stopChangeListeners() {
    logger.info("Stopping change listeners");
    changeTracker.stop();
  }
}
