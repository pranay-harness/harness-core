package io.harness.grpc;

import static io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import static io.harness.delegate.DelegateServiceGrpc.newBlockingStub;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.harness.govern.ProviderModule;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.security.ServiceTokenGenerator;

public class DelegateServiceGrpcClientModule extends ProviderModule {
  private final String serviceSecret;

  public DelegateServiceGrpcClientModule(String serviceSecret) {
    this.serviceSecret = serviceSecret;
  }

  @Provides
  @Singleton
  DelegateServiceBlockingStub delegateServiceBlockingStub(
      @Named("manager-channel") Channel channel, @Named("ds-call-credentials") CallCredentials callCredentials) {
    return newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Named("ds-call-credentials")
  @Provides
  @Singleton
  CallCredentials callCredentials() {
    return new ServiceAuthCallCredentials(serviceSecret, new ServiceTokenGenerator(), "delegate-service");
  }
}
