/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.delegateservice;

import static io.harness.delegate.DelegateServiceGrpc.newBlockingStub;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import io.harness.grpc.DelegateServiceGrpcAgentClient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.CallCredentials;
import io.grpc.Channel;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
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
  DelegateServiceBlockingStub delegateServiceBlockingStub(
      @Named("manager-channel") Channel channel, CallCredentials callCredentials) {
    return newBlockingStub(channel).withCallCredentials(callCredentials);
  }
}
