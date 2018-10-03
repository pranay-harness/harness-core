package io.harness.app;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import io.harness.mongo.MongoQueue;
import io.harness.queue.Queue;
import org.mongodb.morphia.AdvancedDatastore;
import software.wings.waitnotify.NotifyEvent;

/**
 * Verification Queue Guice module for binding queue related services.
 * Created by Raghu on 09/18/18
 */
public class VerificationQueueModule extends AbstractModule {
  private AdvancedDatastore datastore;
  private boolean filterWithVersion;

  /**
   * Creates a guice module for portal app.
   *
   * @param datastore datastore for queues
   */
  public VerificationQueueModule(AdvancedDatastore datastore, boolean filterWithVersion) {
    this.datastore = datastore;
    this.filterWithVersion = filterWithVersion;
  }

  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(new TypeLiteral<Queue<NotifyEvent>>() {})
        .toInstance(new MongoQueue<>(NotifyEvent.class, datastore, 5, filterWithVersion));
  }
}
