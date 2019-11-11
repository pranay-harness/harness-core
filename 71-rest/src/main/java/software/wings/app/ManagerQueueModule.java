package software.wings.app;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import io.harness.config.PublisherConfiguration;
import io.harness.event.model.GenericEvent;
import io.harness.mongo.queue.QueueFactory;
import io.harness.queue.Queue;
import io.harness.queue.QueueListener;
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
  private PublisherConfiguration publisherConfiguration;

  public ManagerQueueModule(PublisherConfiguration configuration) {
    publisherConfiguration = configuration;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<Queue<PruneEvent>>() {})
        .toInstance(QueueFactory.createQueue(PruneEvent.class, publisherConfiguration));
    bind(new TypeLiteral<Queue<EmailData>>() {})
        .toInstance(QueueFactory.createQueue(EmailData.class, publisherConfiguration));
    bind(new TypeLiteral<Queue<CollectEvent>>() {})
        .toInstance(QueueFactory.createQueue(CollectEvent.class, publisherConfiguration));
    bind(new TypeLiteral<Queue<DeploymentEvent>>() {})
        .toInstance(QueueFactory.createQueue(DeploymentEvent.class, ofMinutes(1), true, publisherConfiguration));
    bind(new TypeLiteral<Queue<KmsTransitionEvent>>() {})
        .toInstance(QueueFactory.createQueue(KmsTransitionEvent.class, ofSeconds(30), publisherConfiguration));
    bind(new TypeLiteral<Queue<ExecutionEvent>>() {})
        .toInstance(QueueFactory.createQueue(ExecutionEvent.class, ofSeconds(30), true, publisherConfiguration));
    bind(new TypeLiteral<Queue<DelayEvent>>() {})
        .toInstance(QueueFactory.createQueue(DelayEvent.class, ofSeconds(5), true, publisherConfiguration));
    bind(new TypeLiteral<Queue<GenericEvent>>() {})
        .toInstance(QueueFactory.createQueue(GenericEvent.class, ofMinutes(1), true, publisherConfiguration));
    bind(new TypeLiteral<Queue<InstanceEvent>>() {})
        .toInstance(QueueFactory.createQueue(InstanceEvent.class, ofMinutes(1), true, publisherConfiguration));
    bind(new TypeLiteral<Queue<DeploymentTimeSeriesEvent>>() {})
        .toInstance(
            QueueFactory.createQueue(DeploymentTimeSeriesEvent.class, ofMinutes(1), true, publisherConfiguration));

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
