/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc;

import io.harness.delegate.DelegateServiceGrpc;
import io.harness.delegateprofile.DelegateProfileServiceGrpc;
import io.harness.govern.ProviderModule;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.grpc.pingpong.DelegateServicePingPongClient;
import io.harness.pingpong.DelegateServicePingPongGrpc;
import io.harness.security.ServiceTokenGenerator;
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
import java.util.function.BooleanSupplier;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelegateServiceManagementDriverGrpcClientModule extends ProviderModule {
  private final String serviceSecret;
  private final String target;
  private final String authority;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");
  private final Boolean delegateDriverInstalledInNgService;

  public DelegateServiceManagementDriverGrpcClientModule(
      String serviceSecret, String target, String authority, boolean delegateDriverInstalledInNgService) {
    this.serviceSecret = serviceSecret;
    this.target = target;
    this.authority = authority;
    this.delegateDriverInstalledInNgService = delegateDriverInstalledInNgService;
  }

  @Override
  protected void configure() {
    bind(DelegateServiceGrpcClient.class).in(Singleton.class);
  }

  @Named("delegate-service-management-channel")
  @Singleton
  @Provides
  public Channel delegateServiceManagementChannel(VersionInfoManager versionInfoManager) throws SSLException {
    String authorityToUse = computeAuthority(versionInfoManager.getVersionInfo());
    if ("ONPREM".equals(deployMode) || "KUBERNETES_ONPREM".equals(deployMode)) {
      return NettyChannelBuilder.forTarget(target).overrideAuthority(authorityToUse).usePlaintext().build();
    } else {
      SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      return NettyChannelBuilder.forTarget(target).overrideAuthority(authorityToUse).sslContext(sslContext).build();
    }
  }

  private String computeAuthority(VersionInfo versionInfo) {
    String defaultAuthority = "default-authority.harness.io";
    String authorityToUse;
    if (!isValidAuthority(authority)) {
      log.info("Authority in config {} is invalid. Using default value {}", authority, defaultAuthority);
      authorityToUse = defaultAuthority;
    } else if (!("ONPREM".equals(deployMode) || "KUBERNETES_ONPREM".equals(deployMode))) {
      String versionPrefix = "v-" + versionInfo.getVersion().replace('.', '-') + "-";
      String versionedAuthority = versionPrefix + authority;
      if (isValidAuthority(versionedAuthority)) {
        log.info("Using versioned authority: {}", versionedAuthority);
        authorityToUse = versionedAuthority;
      } else {
        log.info("Versioned authority {} is invalid. Using non-versioned", versionedAuthority);
        authorityToUse = authority;
      }
    } else {
      log.info("Deploy Mode is {}. Using non-versioned authority", deployMode);
      authorityToUse = authority;
    }
    return authorityToUse;
  }

  private static boolean isValidAuthority(String authority) {
    try {
      GrpcUtil.checkAuthority(authority);
      return true;
    } catch (Exception var2) {
      log.error("Exception occurred when checking for valid authority", var2);
      return false;
    }
  }

  @Provides
  @Singleton
  DelegateServiceGrpc.DelegateServiceBlockingStub delegateServiceBlockingStub(
      @Named("delegate-service-management-channel") Channel channel,
      @Named("dsm-call-credentials") CallCredentials callCredentials) {
    return DelegateServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Named("dsm-call-credentials")
  @Provides
  @Singleton
  CallCredentials dsmCallCredentials() {
    return new ServiceAuthCallCredentials(serviceSecret, new ServiceTokenGenerator(), "delegate-service-management");
  }

  @Provides
  @Singleton
  DelegateServicePingPongGrpc.DelegateServicePingPongBlockingStub delegateServicePingPongBlockingStub(
      @Named("delegate-service-management-channel") Channel channel,
      @Named("dspp-call-credentials") CallCredentials callCredentials) {
    return DelegateServicePingPongGrpc.newBlockingStub(channel);
  }

  @Named("dspp-call-credentials")
  @Provides
  @Singleton
  CallCredentials dsppCallCredentials() {
    return new ServiceAuthCallCredentials(serviceSecret, new ServiceTokenGenerator(), "delegate-service-ping-pong");
  }

  @Provides
  @Singleton
  DelegateProfileServiceGrpc.DelegateProfileServiceBlockingStub delegateProfileServiceBlockingStub(
      @Named("delegate-service-management-channel") Channel channel,
      @Named("dps-call-credentials") CallCredentials callCredentials) {
    return DelegateProfileServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Named("dps-call-credentials")
  @Provides
  @Singleton
  CallCredentials dpsCallCredentials() {
    return new ServiceAuthCallCredentials(serviceSecret, new ServiceTokenGenerator(), "delegate-profile-service");
  }

  @Named("driver-installed-in-ng-service")
  @Provides
  @Singleton
  BooleanSupplier isDelegateDriverInstalledInNgServiceSupplier() {
    return () -> delegateDriverInstalledInNgService;
  }

  @Provides
  @Singleton
  DelegateServicePingPongClient delegateServicePingPongClient(
      DelegateServicePingPongGrpc.DelegateServicePingPongBlockingStub pingPongServiceBlockingStub,
      VersionInfoManager versionInfoManager) {
    return new DelegateServicePingPongClient(
        pingPongServiceBlockingStub, versionInfoManager.getVersionInfo().getVersion());
  }
}
