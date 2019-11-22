package software.wings.app;

import static io.harness.queue.Queue.VersionType.UNVERSIONED;
import static io.harness.queue.Queue.VersionType.VERSIONED;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.AbstractModule;
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
  private PublisherConfiguration config;

  public ManagerQueueModule(PublisherConfiguration configuration) {
    config = configuration;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueuePublisher<PruneEvent>>() {})
        .toInstance(QueueFactory.createQueuePublisher(PruneEvent.class, UNVERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<PruneEvent>>() {})
        .toInstance(QueueFactory.createQueueConsumer(PruneEvent.class, UNVERSIONED, ofSeconds(5), config));

    bind(new TypeLiteral<QueuePublisher<EmailData>>() {})
        .toInstance(QueueFactory.createQueuePublisher(EmailData.class, UNVERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<EmailData>>() {})
        .toInstance(QueueFactory.createQueueConsumer(EmailData.class, UNVERSIONED, ofSeconds(5), config));

    bind(new TypeLiteral<QueuePublisher<CollectEvent>>() {})
        .toInstance(QueueFactory.createQueuePublisher(CollectEvent.class, UNVERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<CollectEvent>>() {})
        .toInstance(QueueFactory.createQueueConsumer(CollectEvent.class, UNVERSIONED, ofSeconds(5), config));

    bind(new TypeLiteral<QueuePublisher<DeploymentEvent>>() {})
        .toInstance(QueueFactory.createQueuePublisher(DeploymentEvent.class, VERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<DeploymentEvent>>() {})
        .toInstance(QueueFactory.createQueueConsumer(DeploymentEvent.class, VERSIONED, ofMinutes(1), config));

    bind(new TypeLiteral<QueuePublisher<KmsTransitionEvent>>() {})
        .toInstance(QueueFactory.createQueuePublisher(KmsTransitionEvent.class, UNVERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<KmsTransitionEvent>>() {})
        .toInstance(QueueFactory.createQueueConsumer(KmsTransitionEvent.class, UNVERSIONED, ofSeconds(30), config));

    bind(new TypeLiteral<QueuePublisher<ExecutionEvent>>() {})
        .toInstance(QueueFactory.createQueuePublisher(ExecutionEvent.class, VERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<ExecutionEvent>>() {})
        .toInstance(QueueFactory.createQueueConsumer(ExecutionEvent.class, VERSIONED, ofSeconds(30), config));

    bind(new TypeLiteral<QueuePublisher<DelayEvent>>() {})
        .toInstance(QueueFactory.createQueuePublisher(DelayEvent.class, VERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<DelayEvent>>() {})
        .toInstance(QueueFactory.createQueueConsumer(DelayEvent.class, VERSIONED, ofSeconds(5), config));

    bind(new TypeLiteral<QueuePublisher<GenericEvent>>() {})
        .toInstance(QueueFactory.createQueuePublisher(GenericEvent.class, VERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<GenericEvent>>() {})
        .toInstance(QueueFactory.createQueueConsumer(GenericEvent.class, VERSIONED, ofMinutes(1), config));

    bind(new TypeLiteral<QueuePublisher<InstanceEvent>>() {})
        .toInstance(QueueFactory.createQueuePublisher(InstanceEvent.class, VERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<InstanceEvent>>() {})
        .toInstance(QueueFactory.createQueueConsumer(InstanceEvent.class, VERSIONED, ofMinutes(1), config));

    bind(new TypeLiteral<QueuePublisher<DeploymentTimeSeriesEvent>>() {})
        .toInstance(QueueFactory.createQueuePublisher(DeploymentTimeSeriesEvent.class, VERSIONED, config));
    bind(new TypeLiteral<QueueConsumer<DeploymentTimeSeriesEvent>>() {})
        .toInstance(QueueFactory.createQueueConsumer(DeploymentTimeSeriesEvent.class, VERSIONED, ofMinutes(1), config));

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
