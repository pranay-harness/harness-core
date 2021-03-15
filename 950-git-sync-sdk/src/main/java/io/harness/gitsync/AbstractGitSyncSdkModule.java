package io.harness.gitsync;

import io.harness.EntityType;
import io.harness.grpc.client.GrpcClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractGitSyncSdkModule extends AbstractModule {
  @Override
  protected void configure() {
    install(GitSyncSdkModule.getInstance());
  }

  public abstract GitSyncSdkConfiguration getGitSyncSdkConfiguration();

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
