package io.harness.grpc.delegateservice;

import static io.harness.delegate.DelegateServiceGrpc.newBlockingStub;

import io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.grpc.DelegateServiceGrpcAgentClient;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.CallCredentials;
import io.grpc.Channel;

public class DelegateServiceGrpcAgentClientModule extends AbstractModule {
  private static DelegateServiceGrpcAgentClientModule instance;

  public static DelegateServiceGrpcAgentClientModule getInstance() {
    if (instance == null) {
      instance = new DelegateServiceGrpcAgentClientModule();
    }
    return instance;
  }

  private DelegateServiceGrpcAgentClientModule() {}

  @Override
  protected void configure() {
    bind(DelegateServiceGrpcAgentClient.class).in(Singleton.class);
  }

  @Named("agent-client-stub")
  @Provides
  @Singleton
  DelegateServiceBlockingStub delegateServiceBlockingStub(@Named("manager-channel") Channel channel,
      @Named("del-service-call-credentials") CallCredentials callCredentials) {
    return newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Named("del-service-call-credentials")
  @Provides
  @Singleton
  CallCredentials callCredentials(DelegateConfiguration delegateConfiguration) {
    return new ServiceAuthCallCredentials(
        delegateConfiguration.getManagerServiceSecret(), new ServiceTokenGenerator(), "delegate-service");
  }
}
