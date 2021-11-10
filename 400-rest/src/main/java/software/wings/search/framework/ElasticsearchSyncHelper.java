package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.mongo.changestreams.ChangeSubscriber;
import io.harness.mongo.changestreams.ChangeTracker;
import io.harness.mongo.changestreams.ChangeTrackingInfo;
import io.harness.persistence.PersistentEntity;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.name.Named;
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
  @Inject private Set<TimeScaleEntity<?>> timeScaleEntities;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;
  @Inject @Named("Search") private ChangeTracker changeTracker;

  void startChangeListeners(ChangeSubscriber changeSubscriber) {
    Set<Class<? extends PersistentEntity>> searchOnlySubscribedClasses = new HashSet<>();
    Set<Class<? extends PersistentEntity>> timeScaleOnlySubscribedClasses = new HashSet<>();
    Set<Class<? extends PersistentEntity>> commonSubscribedClasses = new HashSet<>();
    searchEntities.forEach(searchEntity -> searchOnlySubscribedClasses.addAll(searchEntity.getSubscriptionEntities()));
    timeScaleEntities.forEach(timeScaleEntity -> {
      if (searchOnlySubscribedClasses.contains(timeScaleEntity.getSourceEntityClass())) {
        commonSubscribedClasses.add(timeScaleEntity.getSourceEntityClass());
        searchOnlySubscribedClasses.remove(timeScaleEntity.getSourceEntityClass());
      } else {
        timeScaleOnlySubscribedClasses.add(timeScaleEntity.getSourceEntityClass());
      }
    });

    Set<ChangeTrackingInfo<?>> changeTrackingInfos = new HashSet<>();

    for (Class<? extends PersistentEntity> subscribedClass : searchOnlySubscribedClasses) {
      ChangeTrackingInfo<?> changeTrackingInfo = getSearchChangeTrackingInfo(subscribedClass, changeSubscriber);
      changeTrackingInfos.add(changeTrackingInfo);
    }

    for (Class<? extends PersistentEntity> subscribedClass : timeScaleOnlySubscribedClasses) {
      ChangeTrackingInfo<?> changeTrackingInfo = getTimeScaleChangeTrackingInfo(subscribedClass, changeSubscriber);
      changeTrackingInfos.add(changeTrackingInfo);
    }

    for (Class<? extends PersistentEntity> subscribedClass : commonSubscribedClasses) {
      ChangeTrackingInfo<?> changeTrackingInfo = getCommonChangeTrackingInfo(subscribedClass, changeSubscriber);
      changeTrackingInfos.add(changeTrackingInfo);
    }

    log.info("Calling change tracker to start change listeners");
    changeTracker.start(changeTrackingInfos);
  }

  private <T extends PersistentEntity> ChangeTrackingInfo<T> getCommonChangeTrackingInfo(
      Class<T> subscribedClass, ChangeSubscriber<T> changeSubscriber) {
    String token = null;
    if (featureFlagService.isGlobalEnabled(FeatureName.USE_SEARCH_RESUME_TOKEN)) {
      SearchSourceEntitySyncState searchSourceEntitySyncState =
          wingsPersistence.get(SearchSourceEntitySyncState.class, subscribedClass.getCanonicalName());
      if (searchSourceEntitySyncState != null) {
        token = searchSourceEntitySyncState.getLastSyncedToken();
      }
    } else {
      TimeScaleSourceEntitySyncState timeScaleSourceEntitySyncState =
          wingsPersistence.get(TimeScaleSourceEntitySyncState.class, subscribedClass.getCanonicalName());
      if (timeScaleSourceEntitySyncState != null) {
        token = timeScaleSourceEntitySyncState.getLastSyncedToken();
      }
    }

    return new ChangeTrackingInfo<>(subscribedClass, changeSubscriber, token, null);
  }

  private <T extends PersistentEntity> ChangeTrackingInfo<T> getSearchChangeTrackingInfo(
      Class<T> subscribedClass, ChangeSubscriber<T> changeSubscriber) {
    String token = null;
    SearchSourceEntitySyncState searchSourceEntitySyncState =
        wingsPersistence.get(SearchSourceEntitySyncState.class, subscribedClass.getCanonicalName());
    if (searchSourceEntitySyncState != null) {
      token = searchSourceEntitySyncState.getLastSyncedToken();
    }

    return new ChangeTrackingInfo<>(subscribedClass, changeSubscriber, token, null);
  }

  private <T extends PersistentEntity> ChangeTrackingInfo<T> getTimeScaleChangeTrackingInfo(
      Class<T> subscribedClass, ChangeSubscriber<T> changeSubscriber) {
    String token = null;
    TimeScaleSourceEntitySyncState timeScaleSourceEntitySyncState =
        wingsPersistence.get(TimeScaleSourceEntitySyncState.class, subscribedClass.getCanonicalName());
    if (timeScaleSourceEntitySyncState != null) {
      token = timeScaleSourceEntitySyncState.getLastSyncedToken();
    }

    return new ChangeTrackingInfo<>(subscribedClass, changeSubscriber, token, null);
  }

  boolean checkIfAnyChangeListenerIsAlive() {
    return changeTracker.checkIfAnyChangeTrackerIsAlive();
  }

  void stopChangeListeners() {
    log.info("Stopping change listeners");
    changeTracker.stop();
  }
}
