package io.harness.pms.sdk;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServer;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceBlockingStub;
import io.harness.pms.contracts.plan.PmsServiceGrpc;
import io.harness.pms.contracts.plan.PmsServiceGrpc.PmsServiceBlockingStub;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc.EngineExpressionProtoServiceBlockingStub;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc.OutcomeProtoServiceBlockingStub;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc.PmsExecutionServiceBlockingStub;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub;
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
  private final PmsSdkConfiguration config;
  private static PmsSdkGrpcModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

  public static PmsSdkGrpcModule getInstance(PmsSdkConfiguration config) {
    if (instance == null) {
      instance = new PmsSdkGrpcModule(config);
    }
    return instance;
  }

  private PmsSdkGrpcModule(PmsSdkConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class, Names.named("pmsServices"));
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("pms-sdk-grpc-service")));
  }

  @Provides
  @Singleton
  @Named("pms-sdk-grpc-service")
  public Service pmsSdkGrpcService(HealthStatusManager healthStatusManager, PlanCreatorService planCreatorService) {
    Set<BindableService> cdServices = new HashSet<>();
    cdServices.add(healthStatusManager.getHealthService());
    cdServices.add(planCreatorService);
    return new GrpcServer(
        config.getGrpcServerConfig().getConnectors().get(0), cdServices, Collections.emptySet(), healthStatusManager);
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

  private Channel getChannel(GrpcClientConfig clientConfig) throws SSLException {
    String authorityToUse = clientConfig.getAuthority();
    Channel channel;

    if (("ONPREM".equals(deployMode) || "KUBERNETES_ONPREM".equals(deployMode))) {
      channel = NettyChannelBuilder.forTarget(clientConfig.getTarget())
                    .overrideAuthority(authorityToUse)
                    .usePlaintext()
                    .build();
    } else {
      SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      channel = NettyChannelBuilder.forTarget(clientConfig.getTarget())
                    .overrideAuthority(authorityToUse)
                    .sslContext(sslContext)
                    .build();
    }

    return channel;
  }

  @Provides
  @Singleton
  public PmsServiceBlockingStub pmsGrpcClient() throws SSLException {
    GrpcClientConfig clientConfig = config.getPmsGrpcClientConfig();
    return PmsServiceGrpc.newBlockingStub(getChannel(clientConfig));
  }

  @Provides
  @Singleton
  public SweepingOutputServiceBlockingStub sweepingOutputGrpcClient() throws SSLException {
    GrpcClientConfig clientConfig = config.getPmsGrpcClientConfig();
    return SweepingOutputServiceGrpc.newBlockingStub(getChannel(clientConfig));
  }

  @Provides
  @Singleton
  public NodeExecutionProtoServiceBlockingStub nodeExecutionProtoGrpcClient() throws SSLException {
    GrpcClientConfig clientConfig = config.getPmsGrpcClientConfig();
    return NodeExecutionProtoServiceGrpc.newBlockingStub(getChannel(clientConfig));
  }

  @Provides
  @Singleton
  public OutcomeProtoServiceBlockingStub outcomeGrpcClient() throws SSLException {
    GrpcClientConfig clientConfig = config.getPmsGrpcClientConfig();
    return OutcomeProtoServiceGrpc.newBlockingStub(getChannel(clientConfig));
  }

  @Provides
  @Singleton
  public PmsExecutionServiceBlockingStub executionServiceGrpcClient() throws SSLException {
    GrpcClientConfig clientConfig = config.getPmsGrpcClientConfig();
    return PmsExecutionServiceGrpc.newBlockingStub(getChannel(clientConfig));
  }

  @Provides
  @Singleton
  public EngineExpressionProtoServiceBlockingStub engineExpressionGrpcClient() throws SSLException {
    GrpcClientConfig clientConfig = config.getPmsGrpcClientConfig();
    return EngineExpressionProtoServiceGrpc.newBlockingStub(getChannel(clientConfig));
  }

  @Provides
  @Singleton
  @Named("pmsSDKServiceManager")
  public ServiceManager serviceManager(@Named("pmsServices") Set<Service> services) {
    return new ServiceManager(services);
  }
}
