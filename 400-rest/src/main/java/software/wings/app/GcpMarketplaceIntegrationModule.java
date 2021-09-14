/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.app;

import io.harness.marketplace.gcp.GcpMarketPlaceApiHandler;
import io.harness.marketplace.gcp.GcpMarketPlaceApiHandlerImpl;
import io.harness.marketplace.gcp.signup.GcpMarketplaceSignUpHandler;
import io.harness.marketplace.gcp.signup.NewUserRegistrationHandler;
import io.harness.marketplace.gcp.signup.annotations.NewSignUp;

import software.wings.service.impl.marketplace.gcp.GCPBillingPollingServiceImpl;
import software.wings.service.impl.marketplace.gcp.GCPMarketPlaceServiceImpl;
import software.wings.service.impl.marketplace.gcp.GCPUsageReportServiceImpl;
import software.wings.service.intfc.marketplace.gcp.GCPBillingPollingService;
import software.wings.service.intfc.marketplace.gcp.GCPMarketPlaceService;
import software.wings.service.intfc.marketplace.gcp.GCPUsageReportService;

import com.google.inject.AbstractModule;

public final class GcpMarketplaceIntegrationModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(GcpMarketPlaceApiHandler.class).to(GcpMarketPlaceApiHandlerImpl.class);
    bind(GCPUsageReportService.class).to(GCPUsageReportServiceImpl.class);
    bind(GCPBillingPollingService.class).to(GCPBillingPollingServiceImpl.class);
    bind(GCPMarketPlaceService.class).to(GCPMarketPlaceServiceImpl.class);

    bind(GcpMarketplaceSignUpHandler.class).annotatedWith(NewSignUp.class).to(NewUserRegistrationHandler.class);
  }
}
