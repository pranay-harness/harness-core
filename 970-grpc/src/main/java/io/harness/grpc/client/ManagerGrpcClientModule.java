/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.client;

import io.harness.govern.ProviderModule;
import io.harness.grpc.auth.DelegateAuthCallCredentials;
import io.harness.security.TokenGenerator;
import io.harness.version.VersionInfo;
import io.harness.version.VersionInfoManager;

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
import javax.net.ssl.SSLException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Defines plumbing to connect to manager via grpc.
 */
@Slf4j
public class ManagerGrpcClientModule extends ProviderModule {
  private static ManagerGrpcClientModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

  public static ManagerGrpcClientModule getInstance() {
    if (instance == null) {
      instance = new ManagerGrpcClientModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  CallCredentials callCredentials(Config config, @Named("Application") String application) {
    boolean isSsl = isSsl(config, application);
    if (!isSsl) {
      return new DelegateAuthCallCredentials(
          new TokenGenerator(config.accountId, config.accountSecret), config.accountId, false);
    }
    return new DelegateAuthCallCredentials(
        new TokenGenerator(config.accountId, config.accountSecret), config.accountId, true);
  }

  @Named("manager-channel")
  @Singleton
  @Provides
  public Channel managerChannel(Config config, @Named("Application") String application,
      VersionInfoManager versionInfoManager) throws SSLException {
    String authorityToUse = computeAuthority(config, versionInfoManager.getVersionInfo());
    boolean isSsl = isSsl(config, application);

    if (!isSsl) {
      return NettyChannelBuilder.forTarget(config.target).overrideAuthority(authorityToUse).usePlaintext().build();
    }

    SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    return NettyChannelBuilder.forTarget(config.target)
        .overrideAuthority(authorityToUse)
        .sslContext(sslContext)
        .build();
  }

  private boolean isSsl(Config config, @Named("Application") String application) {
    if ("ONPREM".equals(deployMode) || "KUBERNETES_ONPREM".equals(deployMode)) {
      if (("Delegate".equalsIgnoreCase(application)) && ("https".equalsIgnoreCase(config.scheme))) {
        return true;
      }
      return false;
    }
    return true;
  }

  private String computeAuthority(Config config, VersionInfo versionInfo) {
    String defaultAuthority = "default-authority.harness.io";
    String authorityToUse;
    if (!isValidAuthority(config.authority)) {
      log.info("Authority in config {} is invalid. Using default value {}", config.authority, defaultAuthority);
      authorityToUse = defaultAuthority;
    } else if (!("ONPREM".equals(deployMode) || "KUBERNETES_ONPREM".equals(deployMode))) {
      String versionPrefix = "v-" + versionInfo.getVersion().replace('.', '-') + "-";
      String versionedAuthority = versionPrefix + config.authority;
      if (isValidAuthority(versionedAuthority) && !config.isVersionCheckDisabled()) {
        log.info("Using versioned authority: {}", versionedAuthority);
        authorityToUse = versionedAuthority;
      } else {
        authorityToUse = config.authority;
        log.info("Versioned authority {} is invalid. Using non-versioned", versionedAuthority);
      }
    } else {
      log.info("Deploy Mode is {}. Using non-versioned authority", deployMode);
      authorityToUse = config.authority;
    }
    return authorityToUse;
  }

  private static boolean isValidAuthority(String authority) {
    try {
      GrpcUtil.checkAuthority(authority);
    } catch (Exception ignore) {
      log.error("Exception occurred when checking for valid authority", ignore);
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
    String scheme;
    boolean versionCheckDisabled;
  }
}
