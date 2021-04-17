package io.harness.gitsync.client;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.GitToHarnessServiceGrpc;
import io.harness.gitsync.GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcInProcessServer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLException;

@OwnedBy(DX)
public class GitSyncSdkGrpcClientModule extends AbstractModule {
  private static GitSyncSdkGrpcClientModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

  public static GitSyncSdkGrpcClientModule getInstance() {
    if (instance == null) {
      instance = new GitSyncSdkGrpcClientModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}

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
  public Map<ModuleType, GitToHarnessServiceBlockingStub> gitToHarnessServiceGrpcClient(
      @Named("GitSyncGrpcClientConfigs") GitSyncClientConfigs clientConfigs) throws SSLException {
    Map<ModuleType, GitToHarnessServiceBlockingStub> map = new HashMap<>();
    map.put(GmsClientConstants.moduleType,
        GitToHarnessServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName("gmsSdkInternal").build()));
    for (Map.Entry<ModuleType, GrpcClientConfig> entry : clientConfigs.getGitSyncGrpcClients().entrySet()) {
      map.put(entry.getKey(), GitToHarnessServiceGrpc.newBlockingStub(getChannel(entry.getValue())));
    }
    return map;
  }
}
