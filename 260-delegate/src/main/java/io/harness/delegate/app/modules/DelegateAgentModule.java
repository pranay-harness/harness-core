package io.harness.delegate.app.modules;

import static io.harness.configuration.DeployMode.DEPLOY_MODE;
import static io.harness.delegate.service.DelegateAgentServiceImpl.getDelegateId;

import io.harness.delegate.app.DelegateGrpcServiceModule;
import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.delegate.task.citasks.CITaskFactoryModule;
import io.harness.delegate.task.k8s.apiclient.KubernetesApiClientFactoryModule;
import io.harness.event.client.impl.EventPublisherConstants;
import io.harness.event.client.impl.appender.AppenderModule;
import io.harness.event.client.impl.appender.AppenderModule.Config;
import io.harness.grpc.delegateservice.DelegateServiceGrpcAgentClientModule;
import io.harness.grpc.pingpong.PingPongModule;
import io.harness.logstreaming.LogStreamingModule;
import io.harness.managerclient.DelegateManagerClientModule;
import io.harness.perpetualtask.PerpetualTaskWorkerModule;
import io.harness.serializer.KryoModule;

import software.wings.delegatetasks.k8s.client.KubernetesClientFactoryModule;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DelegateAgentModule extends AbstractModule {
  private final DelegateConfiguration configuration;

  @Override
  protected void configure() {
    super.configure();

    install(new DelegateHealthModule());
    install(KryoModule.getInstance());
    install(new DelegateKryoModule());

    install(new DelegateManagerClientModule(configuration.getManagerUrl(), configuration.getVerificationServiceUrl(),
        configuration.getCvNextGenUrl(), configuration.getAccountId(), configuration.getAccountSecret()));

    install(new LogStreamingModule(configuration.getLogStreamingServiceBaseUrl()));
    install(new DelegateGrpcClientModule(configuration));

    if (!ImmutableSet.of("ONPREM", "KUBERNETES_ONPREM").contains(System.getenv().get(DEPLOY_MODE))) {
      install(new PingPongModule());
    }

    if (!"ONPREM".equals(System.getenv().get(DEPLOY_MODE))) {
      install(new PerpetualTaskWorkerModule());
    }

    install(KubernetesClientFactoryModule.getInstance());
    install(KubernetesApiClientFactoryModule.getInstance());
    install(new CITaskFactoryModule());

    final String queueFilePath =
        Optional.ofNullable(configuration.getQueueFilePath()).orElse(EventPublisherConstants.DEFAULT_QUEUE_FILE_PATH);
    final Config appenderConfig = Config.builder().queueFilePath(queueFilePath).build();
    install(new AppenderModule(appenderConfig, () -> getDelegateId().orElse("UNREGISTERED")));

    install(new DelegateModule(configuration));

    if (configuration.isGrpcServiceEnabled()) {
      install(DelegateServiceGrpcAgentClientModule.getInstance());
      install(
          new DelegateGrpcServiceModule(configuration.getGrpcServiceConnectorPort(), configuration.getAccountSecret()));
    }

    install(new DelegateTokensModule(configuration));
  }
}
