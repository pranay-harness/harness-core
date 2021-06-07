package io.harness.tracing;

import io.harness.mongo.tracing.Tracer;
import io.harness.threading.ThreadPool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

class PersistenceTracerModule extends AbstractModule {
  private static PersistenceTracerModule instance;

  static PersistenceTracerModule getInstance() {
    if (instance == null) {
      instance = new PersistenceTracerModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(Tracer.class).to(MongoRedisTracer.class).in(Singleton.class);
  }

  @Provides
  @Named(PersistenceTracerConstants.TRACING_THREAD_POOL)
  public ExecutorService executorService() {
    return ThreadPool.create(5, 10, 20L, TimeUnit.SECONDS,
        new ThreadFactoryBuilder().setNameFormat("query-analysis-%d").setPriority(Thread.MIN_PRIORITY).build());
  }
}
