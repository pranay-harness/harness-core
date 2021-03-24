package io.harness;

import io.harness.changestreamsframework.ChangeSubscriber;
import io.harness.changestreamsframework.ChangeTracker;
import io.harness.changestreamsframework.ChangeTrackingInfo;
import io.harness.entities.CDCEntity;
import io.harness.persistence.PersistentEntity;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
class ChangeDataCaptureHelper {
  @Inject private ChangeTracker changeTracker;
  @Inject private Set<CDCEntity<?>> cdcEntities;
  @Inject private WingsPersistence wingsPersistence;

  void startChangeListeners(ChangeSubscriber changeSubscriber) {
    Set<ChangeTrackingInfo<?>> changeTrackingInfos = new HashSet<>();
    Set<Class<? extends PersistentEntity>> subscribedClasses = new HashSet<>();
    cdcEntities.forEach(cdcEntity -> subscribedClasses.add(cdcEntity.getSubscriptionEntity()));

    for (Class<? extends PersistentEntity> subscribedClass : subscribedClasses) {
      log.info("Subscribed Class: {}", subscribedClass);
      ChangeTrackingInfo<?> changeTrackingInfo = getChangeTrackingInfo(subscribedClass, changeSubscriber);
      changeTrackingInfos.add(changeTrackingInfo);
    }

    log.info("Calling change tracker to start change listeners");
    changeTracker.start(changeTrackingInfos);
  }

  private <T extends PersistentEntity> ChangeTrackingInfo<T> getChangeTrackingInfo(
      Class<T> subscribedClass, ChangeSubscriber<T> changeSubscriber) {
    CDCStateEntity cdcStateEntityState = wingsPersistence.get(CDCStateEntity.class, subscribedClass.getCanonicalName());
    String token = null;
    if (cdcStateEntityState != null) {
      token = cdcStateEntityState.getLastSyncedToken();
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
