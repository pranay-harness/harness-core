package io.harness.waiter;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.version.VersionInfoManager;

@Singleton
public final class OrchestrationNotifyEventListener extends NotifyEventListener {
  public static final String ORCHESTRATION = "orchestration";

  @Inject
  public OrchestrationNotifyEventListener(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    super(QueueFactory.createQueueConsumer(injector, NotifyEvent.class, ofSeconds(5),
        asList(asList(versionInfoManager.getVersionInfo().getVersion()), asList(ORCHESTRATION)), config));
  }
}
