package io.harness;

import com.google.inject.AbstractModule;

import com.codahale.metrics.MetricRegistry;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientFactory;
import io.harness.registry.HarnessMetricRegistry;
import io.harness.security.VerificationTokenGenerator;
import io.harness.service.ContinuousVerificationServiceImpl;
import io.harness.service.intfc.ContinuousVerificationService;
import io.prometheus.client.CollectorRegistry;
import software.wings.utils.WingsIntegrationTestConstants;

public class VerificationTestModule extends AbstractModule {
  @Override
  protected void configure() {
    VerificationTokenGenerator tokenGenerator = new VerificationTokenGenerator();
    bind(VerificationTokenGenerator.class).toInstance(tokenGenerator);
    bind(VerificationManagerClient.class)
        .toProvider(new VerificationManagerClientFactory(WingsIntegrationTestConstants.API_BASE + "/", tokenGenerator));
    bind(ContinuousVerificationService.class).to(ContinuousVerificationServiceImpl.class);

    HarnessMetricRegistry harnessMetricRegistry =
        new HarnessMetricRegistry(new MetricRegistry(), CollectorRegistry.defaultRegistry);
    bind(HarnessMetricRegistry.class).toInstance(harnessMetricRegistry);
  }
}
