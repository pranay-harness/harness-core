package io.harness.pms.sdk.core.execution.listeners;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyEventListener;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

// TODO : Lot of upper modules are using this listener for their use they should not do that as this belonged to the PMS
// Have added a new listener for PMS this listener should be deleted the individual services either need to write their
// own listener Or they should put this listener in their own module
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public final class NgOrchestrationNotifyEventListener extends NotifyEventListener {
  public static final String NG_ORCHESTRATION = "ng_orchestration";

  @Inject
  public NgOrchestrationNotifyEventListener(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    super(QueueFactory.createQueueConsumer(injector, NotifyEvent.class, ofSeconds(5),
        asList(asList(versionInfoManager.getVersionInfo().getVersion()), asList(NG_ORCHESTRATION)), config));
  }
}
