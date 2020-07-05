package io.harness;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.client.ManagerGrpcClientModule;
import io.harness.security.ServiceTokenGenerator;

public class NgManagerGrpcClientModule extends AbstractModule {
  private final GrpcClientConfig grpcClientConfig;
  private final String serviceSecret;

  public NgManagerGrpcClientModule(GrpcClientConfig grpcClientConfig, String serviceSecret) {
    this.grpcClientConfig = grpcClientConfig;
    this.serviceSecret = serviceSecret;
  }

  @Override
  protected void configure() {
    install(new ManagerGrpcClientModule(ManagerGrpcClientModule.Config.builder()
                                            .target(grpcClientConfig.getTarget())
                                            .authority(grpcClientConfig.getAuthority())
                                            .build()));
  }

  @Provides
  @Singleton
  @VisibleForTesting
  NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub(@Named("manager-channel") Channel channel,
      @Named("ng-manager-call-credentials") CallCredentials callCredentials) {
    return NgDelegateTaskServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Named("ng-manager-call-credentials")
  @Provides
  @Singleton
  CallCredentials callCredentials() {
    return new ServiceAuthCallCredentials(this.serviceSecret, new ServiceTokenGenerator(), "ng-manager");
  }
}
