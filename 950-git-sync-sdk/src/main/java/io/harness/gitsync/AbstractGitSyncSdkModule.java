package io.harness.gitsync;

import io.harness.grpc.client.GrpcClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public abstract class AbstractGitSyncSdkModule extends AbstractModule {
  @Override
  protected void configure() {
    install(GitSyncSdkModule.getInstance());
  }

  @Provides
  @Singleton
  @Named("GitSyncGrpcClientConfig")
  public GrpcClientConfig grpcClientConfig() {
    return getGitSyncGrpcClientConfig();
  }

  public abstract GrpcClientConfig getGitSyncGrpcClientConfig();
}
