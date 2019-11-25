package software.wings.app;

import static io.harness.queue.Queue.VersionType.UNVERSIONED;
import static io.harness.queue.Queue.VersionType.VERSIONED;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import io.harness.config.PublisherConfiguration;
import io.harness.event.model.GenericEvent;
import io.harness.mongo.queue.QueueFactory;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.queue.QueuePublisher;
import software.wings.api.DeploymentEvent;
import software.wings.api.DeploymentTimeSeriesEvent;
import software.wings.api.InstanceEvent;
import software.wings.api.KmsTransitionEvent;
import software.wings.collect.ArtifactCollectEventListener;
import software.wings.collect.CollectEvent;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.notification.EmailNotificationListener;
import software.wings.prune.PruneEntityListener;
import software.wings.prune.PruneEvent;
import software.wings.service.impl.DelayEvent;
import software.wings.service.impl.DelayEventListener;
import software.wings.service.impl.ExecutionEvent;
import software.wings.service.impl.ExecutionEventListener;
import software.wings.service.impl.event.DeploymentTimeSeriesEventListener;
import software.wings.service.impl.event.GenericEventListener;
import software.wings.service.impl.instance.DeploymentEventListener;
import software.wings.service.impl.instance.InstanceEventListener;
import software.wings.service.impl.security.KmsTransitionEventListener;

public class ManagerQueueModule extends AbstractModule {
  @Provides
  @Singleton
  QueuePublisher<PruneEvent> pruneQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, PruneEvent.class, UNVERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<PruneEvent> pruneQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, PruneEvent.class, UNVERSIONED, ofSeconds(5), config);
  }

  @Provides
  @Singleton
  QueuePublisher<EmailData> emailQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, EmailData.class, UNVERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<EmailData> emailQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, EmailData.class, UNVERSIONED, ofSeconds(5), config);
  }

  @Provides
  @Singleton
  QueuePublisher<CollectEvent> collectQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, CollectEvent.class, UNVERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<CollectEvent> collectQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, CollectEvent.class, UNVERSIONED, ofSeconds(5), config);
  }

  @Provides
  @Singleton
  QueuePublisher<KmsTransitionEvent> kmsTransitionQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, KmsTransitionEvent.class, UNVERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<KmsTransitionEvent> kmsTransitionQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, KmsTransitionEvent.class, UNVERSIONED, ofSeconds(30), config);
  }

  @Provides
  @Singleton
  QueuePublisher<DeploymentEvent> deploymentQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, DeploymentEvent.class, VERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<DeploymentEvent> deploymentQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, DeploymentEvent.class, VERSIONED, ofMinutes(1), config);
  }

  @Provides
  @Singleton
  QueuePublisher<ExecutionEvent> executionQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, ExecutionEvent.class, VERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<ExecutionEvent> executionQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, ExecutionEvent.class, VERSIONED, ofSeconds(30), config);
  }

  @Provides
  @Singleton
  QueuePublisher<DelayEvent> delayQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, DelayEvent.class, VERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<DelayEvent> delayQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, DelayEvent.class, VERSIONED, ofSeconds(5), config);
  }

  @Provides
  @Singleton
  QueuePublisher<GenericEvent> genericQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, GenericEvent.class, VERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<GenericEvent> genericQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, GenericEvent.class, VERSIONED, ofMinutes(1), config);
  }

  @Provides
  @Singleton
  QueuePublisher<InstanceEvent> instanceQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, InstanceEvent.class, VERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<InstanceEvent> instanceQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, InstanceEvent.class, VERSIONED, ofMinutes(1), config);
  }

  @Provides
  @Singleton
  QueuePublisher<DeploymentTimeSeriesEvent> deploymentTimeSeriesQueuePublisher(
      Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, DeploymentTimeSeriesEvent.class, VERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<DeploymentTimeSeriesEvent> deploymentTimeSeriesQueueConsumer(
      Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, DeploymentTimeSeriesEvent.class, VERSIONED, ofMinutes(1), config);
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueueListener<PruneEvent>>() {}).to(PruneEntityListener.class);
    bind(new TypeLiteral<QueueListener<EmailData>>() {}).to(EmailNotificationListener.class);
    bind(new TypeLiteral<QueueListener<CollectEvent>>() {}).to(ArtifactCollectEventListener.class);
    bind(new TypeLiteral<QueueListener<KmsTransitionEvent>>() {}).to(KmsTransitionEventListener.class);
    bind(new TypeLiteral<QueueListener<DeploymentEvent>>() {}).to(DeploymentEventListener.class);
    bind(new TypeLiteral<QueueListener<ExecutionEvent>>() {}).to(ExecutionEventListener.class);
    bind(new TypeLiteral<QueueListener<DelayEvent>>() {}).to(DelayEventListener.class);
    bind(new TypeLiteral<QueueListener<GenericEvent>>() {}).to(GenericEventListener.class);
    bind(new TypeLiteral<QueueListener<InstanceEvent>>() {}).to(InstanceEventListener.class);
    bind(new TypeLiteral<QueueListener<DeploymentTimeSeriesEvent>>() {}).to(DeploymentTimeSeriesEventListener.class);
  }
}
