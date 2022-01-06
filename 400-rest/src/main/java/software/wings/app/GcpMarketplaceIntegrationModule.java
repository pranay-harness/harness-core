/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
