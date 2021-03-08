package io.harness.core.userchangestream;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.changestreams.ChangeSubscriber;
import io.harness.mongo.changestreams.ChangeTracker;
import io.harness.mongo.changestreams.ChangeTrackingInfo;

import software.wings.beans.User;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
class UserMembershipChangeStreamTask implements Runnable {
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("UserMembership") private ChangeTracker changeTracker;
  @Inject private UserMembershipChangeStreamProcessor userMembershipChangeStreamProcessor;

  private ChangeSubscriber<User> getChangeSubscriber() {
    return changeEvent -> {
      boolean success = userMembershipChangeStreamProcessor.processChangeEvent(changeEvent);
      if (!success) {
        stop();
      }
    };
  }

  @Override
  public void run() {
    UserMembershipChangeStreamState userMembershipChangeStreamState =
        wingsPersistence.get(UserMembershipChangeStreamState.class, UserMembershipChangeStreamState.ID_VALUE);
    String token = null;

    if (userMembershipChangeStreamState != null) {
      token = userMembershipChangeStreamState.getLastSyncedToken();
    }
    ChangeTrackingInfo<User> changeTrackingInfo =
        new ChangeTrackingInfo<>(User.class, getChangeSubscriber(), token, null);
    Set<ChangeTrackingInfo<?>> changeTrackingInfos = Collections.singleton(changeTrackingInfo);
    log.info("Calling change tracker to start change listeners");
    changeTracker.start(changeTrackingInfos);

    while (!Thread.currentThread().isInterrupted() && changeTracker.checkIfAnyChangeTrackerIsAlive()) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        log.info("{} interrupted while in sleep", this.getClass().getName(), e);
        Thread.currentThread().interrupt();
        break;
      }
    }
    stop();
  }

  void stop() {
    log.info("Stopping change listeners");
    changeTracker.stop();
  }
}
