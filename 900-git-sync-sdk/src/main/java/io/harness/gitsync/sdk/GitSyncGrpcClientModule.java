package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.GitSyncSdkConfiguration;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
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
@OwnedBy(DX)
public class GitSyncGrpcClientModule extends AbstractModule {
  private final String deployMode = System.getenv().get("DEPLOY_MODE");
  private static volatile GitSyncGrpcClientModule instance;

  public static GitSyncGrpcClientModule getInstance() {
    if (instance == null) {
      instance = new GitSyncGrpcClientModule();
    }
    return instance;
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

  @Provides
  @Singleton
  public HarnessToGitPushInfoServiceBlockingStub gitToHarnessServiceClient(
      @Named("GitSyncGrpcClientConfig") GrpcClientConfig clientConfig, GitSyncSdkConfiguration config)
      throws SSLException {
    //    if (config.getDeployMode() == GitSyncSdkConfiguration.DeployMode.IN_PROCESS) {
    //      return
    //      HarnessToGitPushInfoServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName(GitSyncGrpcConstants.GitSyncSdkInternalService).build());
    //    }

    return HarnessToGitPushInfoServiceGrpc.newBlockingStub(getChannel(clientConfig));
  }
}
