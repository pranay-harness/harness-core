package io.harness.tracing;

import static io.harness.eventsframework.EventsFrameworkConstants.QUERY_ANALYSIS_TOPIC;
import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_KEY;
import static io.harness.mongo.tracing.TracerConstants.ANALYZER_CACHE_NAME;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cache.HarnessCacheManager;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.mongo.tracing.Tracer;
import io.harness.mongo.tracing.TracerConstants;
import io.harness.redis.RedisConfig;
import io.harness.threading.ThreadPool;
import io.harness.version.VersionInfoManager;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

@OwnedBy(HarnessTeam.PIPELINE)
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

  @Provides
  @Named(PersistenceTracerConstants.QUERY_ANALYSIS_PRODUCER)
  public Producer obtainProducer(RedisConfig redisConfig, @Named(TracerConstants.SERVICE_ID) String serviceId) {
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      return NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME);
    } else {
      return RedisProducer.of(
          QUERY_ANALYSIS_TOPIC, redisConfig, EventsFrameworkConstants.QUERY_ANALYSIS_TOPIC_SIZE, serviceId);
    }
  }

  @Provides
  @Named(ANALYZER_CACHE_NAME)
  @Singleton
  public Cache<String, Long> queryAnalysisCache(HarnessCacheManager harnessCacheManager,
      VersionInfoManager versionInfoManager, @Named(TracerConstants.SERVICE_ID) String serviceId) {
    return harnessCacheManager.getCache(format(ANALYZER_CACHE_KEY, serviceId), String.class, Long.class,
        AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.DAYS, 14)),
        versionInfoManager.getVersionInfo().getBuildNo());
  }
}
