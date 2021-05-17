package io.harness.pms.sdk;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcInProcessServer;
import io.harness.grpc.server.GrpcServer;
import io.harness.metrics.service.api.MetricService;
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
import io.harness.pms.sdk.PmsSdkConfiguration.DeployMode;
import io.harness.pms.sdk.core.plan.creation.creators.PlanCreatorService;
import io.harness.pms.utils.PmsConstants;
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
import io.grpc.netty.shaded.io.grpc.netty.InternalNettyChannelBuilder;
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
  public Service pmsSdkGrpcService(
      HealthStatusManager healthStatusManager, PlanCreatorService planCreatorService, MetricService metricService) {
    metricService.addGrpcViews();
    Set<BindableService> cdServices = new HashSet<>();
    cdServices.add(healthStatusManager.getHealthService());
    cdServices.add(planCreatorService);
    if (config.getDeploymentMode() == DeployMode.REMOTE_IN_PROCESS) {
      return new GrpcInProcessServer("pmsSdkInternal", cdServices, Collections.emptySet(), healthStatusManager, true);
    }
    return new GrpcServer(config.getGrpcServerConfig().getConnectors().get(0), cdServices, Collections.emptySet(),
        healthStatusManager, true);
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

  private Channel getChannel() throws SSLException {
    if (config.getDeploymentMode() == DeployMode.REMOTE_IN_PROCESS) {
      return InProcessChannelBuilder.forName(PmsConstants.INTERNAL_SERVICE_NAME).build();
    }

    GrpcClientConfig clientConfig = config.getPmsGrpcClientConfig();
    String authorityToUse = clientConfig.getAuthority();
    NettyChannelBuilder nettyChannelBuilder;
    if ("ONPREM".equals(deployMode) || "KUBERNETES_ONPREM".equals(deployMode)) {
      nettyChannelBuilder = NettyChannelBuilder.forTarget(clientConfig.getTarget())
                                .overrideAuthority(authorityToUse)
                                .usePlaintext()
                                .maxInboundMessageSize(GrpcInProcessServer.GRPC_MAXIMUM_MESSAGE_SIZE);
    } else {
      SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      nettyChannelBuilder = NettyChannelBuilder.forTarget(clientConfig.getTarget())
                                .overrideAuthority(authorityToUse)
                                .sslContext(sslContext)
                                .maxInboundMessageSize(GrpcInProcessServer.GRPC_MAXIMUM_MESSAGE_SIZE);
    }
    InternalNettyChannelBuilder.setStatsRecordRealTimeMetrics(nettyChannelBuilder, true);
    return nettyChannelBuilder.build();
  }

  @Provides
  @Singleton
  public PmsServiceBlockingStub pmsGrpcClient() throws SSLException {
    return PmsServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public SweepingOutputServiceBlockingStub sweepingOutputGrpcClient() throws SSLException {
    return SweepingOutputServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public InterruptProtoServiceBlockingStub interruptProtoGrpcClient() throws SSLException {
    return InterruptProtoServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public OutcomeProtoServiceBlockingStub outcomeGrpcClient() throws SSLException {
    return OutcomeProtoServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public PmsExecutionServiceBlockingStub executionServiceGrpcClient() throws SSLException {
    return PmsExecutionServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public EngineExpressionProtoServiceBlockingStub engineExpressionGrpcClient() throws SSLException {
    return EngineExpressionProtoServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  @Named("pmsSDKServiceManager")
  public ServiceManager serviceManager(@Named("pmsServices") Set<Service> services) {
    return new ServiceManager(services);
  }
}
