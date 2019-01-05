package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import io.harness.event.model.QueableEvent;
import io.harness.mongo.MongoQueue;
import io.harness.queue.Queue;
import io.harness.queue.QueueListener;
import software.wings.api.DeploymentEvent;
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
import software.wings.service.impl.event.GenericEventListener;
import software.wings.service.impl.instance.DeploymentEventListener;
import software.wings.service.impl.security.KmsTransitionEventListener;

public class ManagerQueueModule extends AbstractModule {
  public ManagerQueueModule() {}

  @Override
  protected void configure() {
    bind(new TypeLiteral<Queue<PruneEvent>>() {}).toInstance(new MongoQueue<>(PruneEvent.class));
    bind(new TypeLiteral<Queue<EmailData>>() {}).toInstance(new MongoQueue<>(EmailData.class));
    bind(new TypeLiteral<Queue<CollectEvent>>() {}).toInstance(new MongoQueue<>(CollectEvent.class));
    bind(new TypeLiteral<Queue<DeploymentEvent>>() {}).toInstance(new MongoQueue<>(DeploymentEvent.class, 60, true));
    bind(new TypeLiteral<Queue<KmsTransitionEvent>>() {}).toInstance(new MongoQueue<>(KmsTransitionEvent.class, 30));
    bind(new TypeLiteral<Queue<ExecutionEvent>>() {}).toInstance(new MongoQueue<>(ExecutionEvent.class, 30, true));
    bind(new TypeLiteral<Queue<DelayEvent>>() {}).toInstance(new MongoQueue<>(DelayEvent.class, 5, true));
    bind(new TypeLiteral<Queue<QueableEvent>>() {}).toInstance(new MongoQueue<>(QueableEvent.class, 60, true));

    bind(new TypeLiteral<QueueListener<PruneEvent>>() {}).to(PruneEntityListener.class);
    bind(new TypeLiteral<QueueListener<EmailData>>() {}).to(EmailNotificationListener.class);
    bind(new TypeLiteral<QueueListener<CollectEvent>>() {}).to(ArtifactCollectEventListener.class);
    bind(new TypeLiteral<QueueListener<KmsTransitionEvent>>() {}).to(KmsTransitionEventListener.class);
    bind(new TypeLiteral<QueueListener<DeploymentEvent>>() {}).to(DeploymentEventListener.class);
    bind(new TypeLiteral<QueueListener<ExecutionEvent>>() {}).to(ExecutionEventListener.class);
    bind(new TypeLiteral<QueueListener<DelayEvent>>() {}).to(DelayEventListener.class);
    bind(new TypeLiteral<QueueListener<QueableEvent>>() {}).to(GenericEventListener.class);
  }
}
