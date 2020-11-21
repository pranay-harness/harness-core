package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import software.wings.dl.WingsPersistence;
import software.wings.search.framework.changestreams.ChangeSubscriber;
import software.wings.search.framework.changestreams.ChangeTracker;
import software.wings.search.framework.changestreams.ChangeTrackingInfo;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract template class for both
 * realtime and bulk synchronisation tasks.
 *
 * @author utkarsh
 */

@OwnedBy(PL)
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

    log.info("Calling change tracker to start change listeners");
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
    log.info("Stopping change listeners");
    changeTracker.stop();
  }
}
