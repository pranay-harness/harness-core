/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.app.modules;

import static io.harness.configuration.DeployMode.DEPLOY_MODE;
import static io.harness.configuration.DeployMode.ONPREM;
import static io.harness.configuration.DeployMode.isOnPrem;
import static io.harness.delegate.service.DelegateAgentServiceImpl.getDelegateId;
import static io.harness.grpc.utils.DelegateGrpcConfigExtractor.extractAuthority;
import static io.harness.grpc.utils.DelegateGrpcConfigExtractor.extractTarget;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.app.DelegateGrpcServiceModule;
import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.delegate.task.citasks.CITaskFactoryModule;
import io.harness.delegate.task.k8s.apiclient.KubernetesApiClientFactoryModule;
import io.harness.event.client.impl.EventPublisherConstants;
import io.harness.event.client.impl.appender.AppenderModule;
import io.harness.event.client.impl.appender.AppenderModule.Config;
import io.harness.event.client.impl.tailer.DelegateTailerModule;
import io.harness.grpc.delegateservice.DelegateServiceGrpcAgentClientModule;
import io.harness.grpc.pingpong.PingPongModule;
import io.harness.logstreaming.LogStreamingModule;
import io.harness.managerclient.DelegateManagerClientModule;
import io.harness.perpetualtask.PerpetualTaskWorkerModule;
import io.harness.serializer.KryoModule;

import software.wings.delegatetasks.k8s.client.KubernetesClientFactoryModule;

import com.google.inject.AbstractModule;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
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

    final String deployMode = System.getenv().get(DEPLOY_MODE);
    if (!ONPREM.name().equals(deployMode)) {
      install(new PerpetualTaskWorkerModule());
    }

    final String queueFilePath =
        Optional.ofNullable(configuration.getQueueFilePath()).orElse(EventPublisherConstants.DEFAULT_QUEUE_FILE_PATH);
    if (!isOnPrem(deployMode)) {
      install(new PingPongModule());
      configureCcmEventTailer(queueFilePath);
    } else {
      log.warn("Skipping event publisher and PingPong configuration for on-prem deployment");
    }
    final Config appenderConfig = Config.builder().queueFilePath(queueFilePath).build();
    install(new AppenderModule(appenderConfig, () -> getDelegateId().orElse("UNREGISTERED")));

    install(KubernetesClientFactoryModule.getInstance());
    install(KubernetesApiClientFactoryModule.getInstance());
    install(new CITaskFactoryModule());
    install(new DelegateModule(configuration));

    if (configuration.isGrpcServiceEnabled()) {
      install(DelegateServiceGrpcAgentClientModule.getInstance());
      install(
          new DelegateGrpcServiceModule(configuration.getGrpcServiceConnectorPort(), configuration.getAccountSecret()));
    }

    install(new DelegateTokensModule(configuration));
  }

  private void configureCcmEventTailer(final String queueFilePath) {
    final String managerHostAndPort = System.getenv("MANAGER_HOST_AND_PORT");
    if (isNotBlank(managerHostAndPort)) {
      final DelegateTailerModule.Config tailerConfig =
          DelegateTailerModule.Config.builder()
              .accountId(configuration.getAccountId())
              .accountSecret(configuration.getAccountSecret())
              .queueFilePath(queueFilePath)
              .publishTarget(extractTarget(managerHostAndPort))
              .publishAuthority(extractAuthority(managerHostAndPort, "events"))
              .build();
      install(new DelegateTailerModule(tailerConfig));
    } else {
      log.warn("Unable to configure event publisher configs. Event publisher will be disabled");
    }
  }
}
