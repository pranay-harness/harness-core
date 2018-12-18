package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.lock.Locker;
import io.harness.lock.PersistentLocker;
import io.harness.lock.PersistentNoopLocker;
import lombok.Getter;
import software.wings.app.MainConfiguration;

@Singleton
public class BackgroundSchedulerLocker {
  @Getter private Locker locker;

  @Inject
  public BackgroundSchedulerLocker(MainConfiguration configuration, PersistentLocker persistentLocker) {
    locker = configuration.getBackgroundSchedulerConfig().isClustered() ? new PersistentNoopLocker() : persistentLocker;
  }
}
