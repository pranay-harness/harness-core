package io.harness.waiter;

import static java.util.Arrays.asList;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.queue.QueueModule;
import io.harness.queue.QueuePublisher;
import io.harness.version.VersionInfoManager;

public class WaiterModule extends AbstractModule {
  private static WaiterModule instance;

  public static WaiterModule getInstance() {
    if (instance == null) {
      instance = new WaiterModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  QueuePublisher<NotifyEvent> notifyQueuePublisher(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(
        injector, NotifyEvent.class, asList(versionInfoManager.getVersionInfo().getVersion()), config);
  }

  @Override
  protected void configure() {
    install(QueueModule.getInstance());
  }
}
