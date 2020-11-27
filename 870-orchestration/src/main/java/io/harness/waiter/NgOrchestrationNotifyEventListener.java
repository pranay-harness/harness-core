package io.harness.waiter;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.version.VersionInfoManager;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public final class NgOrchestrationNotifyEventListener extends NotifyEventListener {
  public static final String NG_ORCHESTRATION = "ng_orchestration";

  @Inject
  public NgOrchestrationNotifyEventListener(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    super(QueueFactory.createQueueConsumer(injector, NotifyEvent.class, ofSeconds(5),
        asList(asList(versionInfoManager.getVersionInfo().getVersion()), asList(NG_ORCHESTRATION)), config));
  }
}
