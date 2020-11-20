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
