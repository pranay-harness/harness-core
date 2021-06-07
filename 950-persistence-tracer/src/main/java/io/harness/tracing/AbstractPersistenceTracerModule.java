package io.harness.tracing;

import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_NAME;

import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.redis.DistributedCache;
import io.harness.mongo.tracing.TracerConstants;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import javax.inject.Named;

public abstract class AbstractPersistenceTracerModule extends AbstractModule {
  @Override
  protected void configure() {
    install(PersistenceTracerModule.getInstance());
  }

  @Provides
  @Named(PersistenceTracerConstants.QUERY_ANALYSIS_PRODUCER)
  public Producer obtainProducer() {
    return producerProvider();
  }

  @Provides
  @Named(TracerConstants.SERVICE_ID)
  public String serviceId() {
    return serviceIdProvider();
  }

  @Provides
  @Named(ANALYZER_CACHE_NAME)
  public DistributedCache obtainCache() {
    return cacheProvider();
  }

  protected abstract DistributedCache cacheProvider();

  public abstract Producer producerProvider();

  protected abstract String serviceIdProvider();
}
