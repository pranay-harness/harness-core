/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.app;

import static io.harness.product.ci.scm.proto.SCMGrpc.newBlockingStub;

import io.harness.govern.ProviderModule;
import io.harness.product.ci.scm.proto.SCMGrpc;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import javax.net.ssl.SSLException;

public class SCMGrpcClientModule extends ProviderModule {
  ScmConnectionConfig scmConnectionConfig;

  public SCMGrpcClientModule(ScmConnectionConfig scmConnectionConfig) {
    this.scmConnectionConfig = scmConnectionConfig;
  }

  @Provides
  @Singleton
  SCMGrpc.SCMBlockingStub scmServiceBlockingStub() throws SSLException {
    return newBlockingStub(scmChannel(scmConnectionConfig.getUrl()));
  }

  @Singleton
  @Provides
  public Channel scmChannel(String connectionUrl) {
    // TODO: Authentication Needs to be added here.
    return NettyChannelBuilder.forTarget(connectionUrl).usePlaintext().build();
  }
}
