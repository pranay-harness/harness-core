package io.harness.gitsync;

import io.harness.EntityType;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.scm.SCMGrpcClientModule;
import io.harness.scm.ScmConnectionConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractGitSyncSdkModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new SCMGrpcClientModule(getScmConnectionConfig()));
    install(GitSyncSdkModule.getInstance());
    if (getGitSyncSdkConfiguration().getEventsRedisConfig().getRedisUrl().equals("dummyRedisUrl")) {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
    } else {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH,
              getGitSyncSdkConfiguration().getEventsRedisConfig(),
              EventsFrameworkConstants.HARNESS_TO_GIT_PUSH_MAX_TOPIC_SIZE));
    }
  }

  public abstract GitSyncSdkConfiguration getGitSyncSdkConfiguration();

  @Provides
  @Singleton
  public ScmConnectionConfig getScmConnectionConfig() {
    return getGitSyncSdkConfiguration().getScmConnectionConfig();
  }

  @Provides
  @Singleton
  public GitSyncSdkConfiguration gitSyncSdkConfiguration() {
    return getGitSyncSdkConfiguration();
  }

  @Provides
  @Singleton
  @Named("GitSyncGrpcClientConfig")
  public GrpcClientConfig grpcClientConfig() {
    return getGitSyncSdkConfiguration().getGrpcClientConfig();
  }

  @Provides
  @Singleton
  @Named("GitSyncSortOrder")
  public Supplier<List<EntityType>> getSortOrder() {
    return getGitSyncSdkConfiguration().getGitSyncSortOrder();
  }
}
