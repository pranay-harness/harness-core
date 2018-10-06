package software.wings.rules;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import io.harness.mongo.MongoQueue;
import io.harness.queue.Queue;
import software.wings.api.DeploymentEvent;
import software.wings.api.InstanceChangeEvent;
import software.wings.api.KmsTransitionEvent;
import software.wings.collect.ArtifactCollectEventListener;
import software.wings.collect.CollectEvent;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.notification.EmailNotificationListener;
import software.wings.service.impl.DelayEvent;
import software.wings.service.impl.DelayEventListener;
import software.wings.service.impl.ExecutionEvent;
import software.wings.service.impl.ExecutionEventListener;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.waitnotify.NotifyEvent;
import software.wings.waitnotify.NotifyEventListener;

public class QueueModuleTest extends AbstractModule {
  @Override
  protected void configure() {
    bind(new TypeLiteral<Queue<EmailData>>() {}).toInstance(new MongoQueue<>(EmailData.class));
    bind(new TypeLiteral<Queue<CollectEvent>>() {}).toInstance(new MongoQueue<>(CollectEvent.class));
    bind(new TypeLiteral<Queue<NotifyEvent>>() {}).toInstance(new MongoQueue<>(NotifyEvent.class, 5, false));
    bind(new TypeLiteral<Queue<KmsTransitionEvent>>() {}).toInstance(new MongoQueue<>(KmsTransitionEvent.class, 30));
    bind(new TypeLiteral<Queue<ExecutionEvent>>() {}).toInstance(new MongoQueue<>(ExecutionEvent.class, 30, false));
    bind(new TypeLiteral<Queue<DeploymentEvent>>() {}).toInstance(new MongoQueue<>(DeploymentEvent.class, 60, false));
    bind(new TypeLiteral<Queue<InstanceChangeEvent>>() {}).toInstance(new MongoQueue<>(InstanceChangeEvent.class, 60));
    bind(new TypeLiteral<Queue<DelayEvent>>() {}).toInstance(new MongoQueue<>(DelayEvent.class, 5, false));

    bind(new TypeLiteral<AbstractQueueListener<EmailData>>() {}).to(EmailNotificationListener.class);
    bind(new TypeLiteral<AbstractQueueListener<CollectEvent>>() {}).to(ArtifactCollectEventListener.class);
    bind(new TypeLiteral<AbstractQueueListener<NotifyEvent>>() {}).to(NotifyEventListener.class);
    bind(new TypeLiteral<AbstractQueueListener<KmsTransitionEvent>>() {}).to(KmsTransitionEventListener.class);
    bind(new TypeLiteral<AbstractQueueListener<ExecutionEvent>>() {}).to(ExecutionEventListener.class);
    bind(new TypeLiteral<AbstractQueueListener<DelayEvent>>() {}).to(DelayEventListener.class);
  }
}
