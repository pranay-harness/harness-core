package io.harness.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.lock.Locker;
import io.harness.lock.PersistentLocker;
import io.harness.lock.PersistentNoopLocker;
import lombok.Getter;

@Singleton
public class BackgroundSchedulerLocker {
  @Getter private Locker locker;

  @Inject
  public BackgroundSchedulerLocker(
      PersistentLocker persistentLocker, @Named("BackgroundSchedule") SchedulerConfig configuration) {
    locker = configuration.isClustered() ? new PersistentNoopLocker() : persistentLocker;
  }
}
