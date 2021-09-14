/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientFactory;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.security.ServiceTokenGenerator;
import io.harness.service.ContinuousVerificationServiceImpl;
import io.harness.service.intfc.ContinuousVerificationService;

import software.wings.utils.WingsIntegrationTestConstants;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import io.prometheus.client.CollectorRegistry;

public class VerificationTestModule extends AbstractModule {
  @Override
  protected void configure() {
    ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    bind(ServiceTokenGenerator.class).toInstance(tokenGenerator);
    bind(VerificationManagerClient.class)
        .toProvider(new VerificationManagerClientFactory(WingsIntegrationTestConstants.API_BASE + "/", tokenGenerator));
    bind(ContinuousVerificationService.class).to(ContinuousVerificationServiceImpl.class);

    HarnessMetricRegistry harnessMetricRegistry =
        new HarnessMetricRegistry(new MetricRegistry(), CollectorRegistry.defaultRegistry);
    bind(HarnessMetricRegistry.class).toInstance(harnessMetricRegistry);
  }
}
