package io.harness.grpc;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.security.ServiceTokenGenerator;

public class GrpcClientModule extends AbstractModule {
  private final GrpcClientConfig grpcClientConfig;
  private final String serviceSecret;

  public GrpcClientModule(GrpcClientConfig grpcClientConfig, String serviceSecret) {
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
  NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskResponseServiceBlockingStub(
      @Named("manager-channel") Channel channel, @Named("ng-call-credentials") CallCredentials callCredentials) {
    return NgDelegateTaskResponseServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Named("ng-call-credentials")
  @Provides
  @Singleton
  CallCredentials callCredentials() {
    return new ServiceAuthCallCredentials(this.serviceSecret, new ServiceTokenGenerator(), "manager");
  }
}
