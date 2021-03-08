package io.harness.gitsync.sdk;

import io.harness.gitsync.GitToHarnessServiceGrpc;
import io.harness.gitsync.GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcInProcessServer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitSyncGrpcClientModule extends AbstractModule {
  private final String deployMode = System.getenv().get("DEPLOY_MODE");
  private static volatile GitSyncGrpcClientModule instance;

  public static GitSyncGrpcClientModule getInstance() {
    if (instance == null) {
      instance = new GitSyncGrpcClientModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  public GitToHarnessServiceBlockingStub gitToHarnessServiceClient(
      @Named("GitSyncGrpcClientConfig") GrpcClientConfig clientConfig) throws SSLException {
    return GitToHarnessServiceGrpc.newBlockingStub(getChannel(clientConfig));
  }

  public Channel getChannel(GrpcClientConfig clientConfig) throws SSLException {
    String authorityToUse = clientConfig.getAuthority();
    Channel channel;

    if ("ONPREM".equals(deployMode) || "KUBERNETES_ONPREM".equals(deployMode)) {
      channel = NettyChannelBuilder.forTarget(clientConfig.getTarget())
                    .overrideAuthority(authorityToUse)
                    .usePlaintext()
                    .maxInboundMessageSize(GrpcInProcessServer.GRPC_MAXIMUM_MESSAGE_SIZE)
                    .build();
    } else {
      SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      channel = NettyChannelBuilder.forTarget(clientConfig.getTarget())
                    .overrideAuthority(authorityToUse)
                    .sslContext(sslContext)
                    .maxInboundMessageSize(GrpcInProcessServer.GRPC_MAXIMUM_MESSAGE_SIZE)
                    .build();
    }

    return channel;
  }
}
