package io.harness.app;

import static io.harness.product.ci.scm.proto.SCMGrpc.newBlockingStub;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.harness.govern.ProviderModule;
import io.harness.product.ci.scm.proto.SCMGrpc;

import javax.net.ssl.SSLException;

public class SCMGrpcClientModule extends ProviderModule {
  @Provides
  @Singleton
  SCMGrpc.SCMBlockingStub scmServiceBlockingStub() throws SSLException {
    return newBlockingStub(scmChannel());
  }
  @Singleton
  @Provides
  public Channel scmChannel() {
    // TODO: Update it with appropriate host name and authentication
    return NettyChannelBuilder.forTarget("localhost:8091").usePlaintext().build();
  }
}