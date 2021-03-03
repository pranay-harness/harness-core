package io.harness;

import io.harness.changestreamsframework.ChangeSubscriber;
import io.harness.changestreamsframework.ChangeTracker;
import io.harness.changestreamsframework.ChangeTrackingInfo;
import io.harness.entities.CDCEntity;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ChangeDataCaptureHelper {
  @Inject private ChangeTracker changeTracker;
  @Inject private Set<CDCEntity<?>> cdcEntities;

  void startChangeListeners(ChangeSubscriber changeSubscriber) {
    Set<ChangeTrackingInfo<?>> changeTrackingInfos = new HashSet<>();
    Set<Class<? extends PersistentEntity>> subscribedClasses = new HashSet<>();
    cdcEntities.forEach(cdcEntity -> subscribedClasses.add(cdcEntity.getSubscriptionEntity()));

    for (Class<? extends PersistentEntity> subscribedClass : subscribedClasses) {
      ChangeTrackingInfo<?> changeTrackingInfo = new ChangeTrackingInfo<>(subscribedClass, changeSubscriber);
      changeTrackingInfos.add(changeTrackingInfo);
    }

    log.info("Calling change tracker to start change listeners");
    changeTracker.start(changeTrackingInfos);
  }

  boolean checkIfAnyChangeListenerIsAlive() {
    return changeTracker.checkIfAnyChangeTrackerIsAlive();
  }

  void stopChangeListeners() {
    log.info("Stopping change listeners");
    changeTracker.stop();
  }
}
