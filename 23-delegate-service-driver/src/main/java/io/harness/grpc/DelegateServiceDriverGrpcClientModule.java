package io.harness.grpc;

import static io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import static io.harness.delegateprofile.DelegateProfileServiceGrpc.DelegateProfileServiceBlockingStub;

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
import io.harness.delegate.DelegateServiceGrpc;
import io.harness.delegateprofile.DelegateProfileServiceGrpc;
import io.harness.govern.ProviderModule;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.security.ServiceTokenGenerator;
import io.harness.version.VersionInfo;
import io.harness.version.VersionInfoManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.SSLException;

@Slf4j
public class DelegateServiceDriverGrpcClientModule extends ProviderModule {
  private final String serviceSecret;
  private final String target;
  private final String authority;
  private final String protocol;

  public DelegateServiceDriverGrpcClientModule(String serviceSecret, String target, String authority, String protocol) {
    this.serviceSecret = serviceSecret;
    this.target = target;
    this.authority = authority;
    this.protocol = protocol;
  }

  @Override
  protected void configure() {
    bind(DelegateServiceGrpcClient.class).in(Singleton.class);
    bind(DelegateProfileServiceGrpcClient.class).in(Singleton.class);
  }

  @Named("delegate-service-channel")
  @Singleton
  @Provides
  public Channel managerChannel(VersionInfoManager versionInfoManager) throws SSLException {
    String authorityToUse = computeAuthority(versionInfoManager.getVersionInfo());

    if (StringUtils.isBlank(protocol) || protocol.toLowerCase().startsWith("https")) {
      SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      return NettyChannelBuilder.forTarget(target).overrideAuthority(authorityToUse).sslContext(sslContext).build();
    }

    return NettyChannelBuilder.forTarget(target).overrideAuthority(authorityToUse).usePlaintext().build();
  }

  private String computeAuthority(VersionInfo versionInfo) {
    String defaultAuthority = "default-authority.harness.io";
    String authorityToUse;
    if (!isValidAuthority(authority)) {
      log.info("Authority in config {} is invalid. Using default value {}", authority, defaultAuthority);
      authorityToUse = defaultAuthority;
    } else {
      String versionPrefix = "v-" + versionInfo.getVersion().replace('.', '-') + "-";
      String versionedAuthority = versionPrefix + authority;
      if (isValidAuthority(versionedAuthority)) {
        log.info("Using versioned authority: {}", versionedAuthority);
        authorityToUse = versionedAuthority;
      } else {
        log.info("Versioned authority {} is invalid. Using non-versioned", versionedAuthority);
        authorityToUse = authority;
      }
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
  DelegateServiceBlockingStub delegateServiceBlockingStub(@Named("delegate-service-channel") Channel channel,
      @Named("ds-call-credentials") CallCredentials callCredentials) {
    return DelegateServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Provides
  @Singleton
  DelegateProfileServiceBlockingStub delegateProfileServiceBlockingStub(
      @Named("delegate-service-channel") Channel channel,
      @Named("dps-call-credentials") CallCredentials callCredentials) {
    return DelegateProfileServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Named("ds-call-credentials")
  @Provides
  @Singleton
  CallCredentials dsCallCredentials() {
    return new ServiceAuthCallCredentials(serviceSecret, new ServiceTokenGenerator(), "delegate-service");
  }

  @Named("dps-call-credentials")
  @Provides
  @Singleton
  CallCredentials dpsCallCredentials() {
    return new ServiceAuthCallCredentials(serviceSecret, new ServiceTokenGenerator(), "delegate-profile-service");
  }
}
