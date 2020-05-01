package io.harness.grpc;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.harness.govern.ProviderModule;
import io.harness.grpc.auth.DelegateAuthCallCredentials;
import io.harness.security.TokenGenerator;
import io.harness.version.VersionInfo;
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

  @Provides
  @Singleton
  CallCredentials callCredentials() {
    return new DelegateAuthCallCredentials(
        new TokenGenerator(config.accountId, config.accountSecret), config.accountId, true);
  }

  @Named("manager-channel")
  @Singleton
  @Provides
  public Channel managerChannel(VersionInfoManager versionInfoManager) throws SSLException {
    String authorityToUse = computeAuthority(versionInfoManager.getVersionInfo());
    SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    return NettyChannelBuilder.forTarget(config.target)
        .overrideAuthority(authorityToUse)
        .sslContext(sslContext)
        .build();
  }

  private String computeAuthority(VersionInfo versionInfo) {
    String defaultAuthority = "default-authority.harness.io";
    String authorityToUse;
    if (!isValidAuthority(config.authority)) {
      logger.info("Authority in config {} is invalid. Using default value {}", config.authority, defaultAuthority);
      authorityToUse = defaultAuthority;
    } else {
      String versionPrefix = "v-" + versionInfo.getVersion().replace('.', '-') + "-";
      String versionedAuthority = versionPrefix + config.authority;
      if (isValidAuthority(versionedAuthority)) {
        logger.info("Using versioned authority: {}", versionedAuthority);
        authorityToUse = versionedAuthority;
      } else {
        logger.info("Versioned authority {} is invalid. Using non-versioned", versionedAuthority);
        authorityToUse = config.authority;
      }
    }
    return authorityToUse;
  }

  private static boolean isValidAuthority(String authority) {
    try {
      GrpcUtil.checkAuthority(authority);
    } catch (Exception ignore) {
      return false;
    }
    return true;
  }

  @Value
  @Builder
  public static class Config {
    String target;
    String authority;
    String accountId;
    String accountSecret;
  }
}
