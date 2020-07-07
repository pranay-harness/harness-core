package io.harness;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.grpc.Channel;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.client.ManagerGrpcClientModule;
import io.harness.security.ServiceTokenGenerator;

public class ManagerDelegateServiceDriverModule extends AbstractModule {
  private final GrpcClientConfig grpcClientConfig;
  private final String serviceSecret;
  private final String serviceId;

  public ManagerDelegateServiceDriverModule(GrpcClientConfig grpcClientConfig, String serviceSecret, String serviceId) {
    this.grpcClientConfig = grpcClientConfig;
    this.serviceSecret = serviceSecret;
    this.serviceId = serviceId;
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
  NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub(@Named("manager-channel") Channel channel) {
    ServiceAuthCallCredentials callCredentials =
        new ServiceAuthCallCredentials(this.serviceSecret, new ServiceTokenGenerator(), this.serviceId);
    return NgDelegateTaskServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }
}
