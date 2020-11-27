package io.harness.grpc;

import static io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import static io.harness.delegate.DelegateServiceGrpc.newBlockingStub;

import io.harness.govern.ProviderModule;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.CallCredentials;
import io.grpc.Channel;

public class DelegateServiceGrpcLiteClientModule extends ProviderModule {
  private final String serviceSecret;

  public DelegateServiceGrpcLiteClientModule(String serviceSecret) {
    this.serviceSecret = serviceSecret;
  }

  @Override
  protected void configure() {
    bind(DelegateServiceGrpcLiteClient.class).in(Singleton.class);
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
