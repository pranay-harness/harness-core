package io.harness.grpc;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.grpc.Channel;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.harness.govern.ProviderModule;
import io.harness.version.VersionInfoManager;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;

/**
 * Defines plumbing to connect to manager via grpc.
 */
@Slf4j
public class ManagerGrpcClientModule extends ProviderModule {
  private final Config config;

  public ManagerGrpcClientModule(Config config) {
    this.config = config;
  }

  @Named("manager-channel")
  @Singleton
  @Provides
  public Channel managerChannel(VersionInfoManager versionInfoManager) throws SSLException {
    String versionPrefix = "v-" + versionInfoManager.getVersionInfo().getVersion().replace('.', '-') + "-";
    String versionedAuthority = versionPrefix + config.authority;
    String authorityToUse;
    try {
      authorityToUse = GrpcUtil.checkAuthority(versionedAuthority);
      logger.info("Using versioned authority: {}", versionedAuthority);
    } catch (Exception e) {
      authorityToUse = config.authority;
      logger.error("Using non-versioned authority due to error", e);
    }
    SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    return NettyChannelBuilder.forTarget(config.target)
        .overrideAuthority(authorityToUse)
        .sslContext(sslContext)
        .build();
  }

  @Value
  @Builder
  public static class Config {
    String target;
    String authority;
  }
}
