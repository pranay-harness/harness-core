package io.harness.pms.sdk.core;

import io.harness.ModuleType;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcInProcessServer;
import io.harness.grpc.server.GrpcServer;
import io.harness.pms.contracts.plan.PmsServiceGrpc;
import io.harness.pms.contracts.plan.PmsServiceGrpc.PmsServiceBlockingStub;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc.EngineExpressionProtoServiceBlockingStub;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc.InterruptProtoServiceBlockingStub;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc.OutcomeProtoServiceBlockingStub;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc.PmsExecutionServiceBlockingStub;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub;
import io.harness.pms.sdk.core.execution.expression.RemoteFunctorService;
import io.harness.pms.sdk.core.plan.creation.creators.PlanCreatorService;
import io.harness.version.VersionInfo;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.services.HealthStatusManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PmsSdkGrpcModule extends AbstractModule {
  private final PmsSdkCoreConfig config;
  private static PmsSdkGrpcModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

  public static PmsSdkGrpcModule getInstance(PmsSdkCoreConfig config) {
    if (instance == null) {
      instance = new PmsSdkGrpcModule(config);
    }
    return instance;
  }

  private PmsSdkGrpcModule(PmsSdkCoreConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(new PmsSdkGrpcClientStubModule(config));
  }

  private String computeAuthority(String authority, VersionInfo versionInfo) {
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
    } catch (Exception ignore) {
      log.error("Exception occurred when checking for valid authority", ignore);
      return false;
    }
    return true;
  }
}
