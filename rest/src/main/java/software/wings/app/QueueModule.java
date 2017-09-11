package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import org.mongodb.morphia.AdvancedDatastore;
import software.wings.api.ContainerDeploymentEvent;
import software.wings.api.InstanceChangeEvent;
import software.wings.collect.ArtifactCollectEventListener;
import software.wings.collect.CollectEvent;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.core.queue.MongoQueueImpl;
import software.wings.core.queue.Queue;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.notification.EmailNotificationListener;
import software.wings.service.impl.instance.ContainerDeploymentEventListener;
import software.wings.service.impl.instance.InstanceChangeEventListener;
import software.wings.service.impl.instance.sync.InstanceSyncService;
import software.wings.service.impl.instance.sync.KubernetesInstanceSyncServiceImpl;
import software.wings.waitnotify.NotifyEvent;
import software.wings.waitnotify.NotifyEventListener;

/**
 * Created by peeyushaggarwal on 5/25/16.
 */
public class QueueModule extends AbstractModule {
  private AdvancedDatastore datastore;

  /**
   * Creates a guice module for portal app.
   *
   * @param datastore datastore for queues
   */
  public QueueModule(AdvancedDatastore datastore) {
    this.datastore = datastore;
  }

  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(new TypeLiteral<Queue<EmailData>>() {}).toInstance(new MongoQueueImpl<>(EmailData.class, datastore));
    bind(new TypeLiteral<Queue<CollectEvent>>() {}).toInstance(new MongoQueueImpl<>(CollectEvent.class, datastore));
    bind(new TypeLiteral<Queue<NotifyEvent>>() {}).toInstance(new MongoQueueImpl<>(NotifyEvent.class, datastore));
    bind(new TypeLiteral<Queue<InstanceChangeEvent>>() {})
        .toInstance(new MongoQueueImpl<>(InstanceChangeEvent.class, datastore, 60));
    bind(new TypeLiteral<Queue<ContainerDeploymentEvent>>() {})
        .toInstance(new MongoQueueImpl<>(ContainerDeploymentEvent.class, datastore, 60));

    bind(new TypeLiteral<AbstractQueueListener<EmailData>>() {}).to(EmailNotificationListener.class);
    bind(new TypeLiteral<AbstractQueueListener<CollectEvent>>() {}).to(ArtifactCollectEventListener.class);
    bind(new TypeLiteral<AbstractQueueListener<NotifyEvent>>() {}).to(NotifyEventListener.class);
    bind(new TypeLiteral<AbstractQueueListener<InstanceChangeEvent>>() {}).to(InstanceChangeEventListener.class);
    bind(new TypeLiteral<AbstractQueueListener<ContainerDeploymentEvent>>() {})
        .to(ContainerDeploymentEventListener.class);
  }
}
