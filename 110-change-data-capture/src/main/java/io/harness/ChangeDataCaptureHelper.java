package io.harness;

import io.harness.changestreamsframework.ChangeSubscriber;
import io.harness.changestreamsframework.ChangeTracker;
import io.harness.changestreamsframework.ChangeTrackingInfo;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class ChangeDataCaptureHelper {
  @Inject private ChangeTracker changeTracker;

  void startChangeListeners(
      ChangeSubscriber changeSubscriber, Set<Class<? extends PersistentEntity>> subscribedClasses) {
    Set<ChangeTrackingInfo<?>> changeTrackingInfos = new HashSet<>();

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
