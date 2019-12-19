package software.wings.app;

import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyEventListener;

@Singleton
public final class GeneralNotifyEventListener extends NotifyEventListener {
  @Inject
  public GeneralNotifyEventListener(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    super(QueueFactory.createQueueConsumer(injector, NotifyEvent.class, ofSeconds(5),
        asList(asList(versionInfoManager.getVersionInfo().getVersion()), asList(GENERAL)), config));
  }
}
