package io.harness;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcInProcessServer;
import io.harness.pms.contracts.service.EntityReferenceServiceGrpc;
import io.harness.pms.contracts.service.EntityReferenceServiceGrpc.EntityReferenceServiceBlockingStub;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javax.net.ssl.SSLException;

public class TemplateServiceGrpcClientModule extends AbstractModule {
  private static TemplateServiceGrpcClientModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

  public static TemplateServiceGrpcClientModule getInstance() {
    if (instance == null) {
      instance = new TemplateServiceGrpcClientModule();
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
  public EntityReferenceServiceBlockingStub entityReferenceServiceClient(TemplateServiceConfiguration configuration)
      throws SSLException {
    return EntityReferenceServiceGrpc.newBlockingStub(getChannel(configuration.getPmsGrpcClientConfig()));
  }
}
