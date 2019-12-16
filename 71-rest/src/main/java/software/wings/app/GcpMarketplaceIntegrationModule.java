package software.wings.app;

import com.google.inject.AbstractModule;

import io.harness.marketplace.gcp.GcpMarketPlaceApiHandler;
import io.harness.marketplace.gcp.GcpMarketPlaceApiHandlerImpl;
import io.harness.marketplace.gcp.signup.ExistingUserRegistrationHandler;
import io.harness.marketplace.gcp.signup.GcpMarketplaceSignUpHandler;
import io.harness.marketplace.gcp.signup.NewUserRegistrationHandler;
import io.harness.marketplace.gcp.signup.annotations.NewSignUp;
import io.harness.marketplace.gcp.signup.annotations.ReturningUser;
import software.wings.service.impl.marketplace.gcp.GCPBillingPollingServiceImpl;
import software.wings.service.impl.marketplace.gcp.GCPMarketPlaceServiceImpl;
import software.wings.service.impl.marketplace.gcp.GCPUsageReportServiceImpl;
import software.wings.service.intfc.marketplace.gcp.GCPBillingPollingService;
import software.wings.service.intfc.marketplace.gcp.GCPMarketPlaceService;
import software.wings.service.intfc.marketplace.gcp.GCPUsageReportService;

public final class GcpMarketplaceIntegrationModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(GcpMarketPlaceApiHandler.class).to(GcpMarketPlaceApiHandlerImpl.class);
    bind(GCPUsageReportService.class).to(GCPUsageReportServiceImpl.class);
    bind(GCPBillingPollingService.class).to(GCPBillingPollingServiceImpl.class);
    bind(GCPMarketPlaceService.class).to(GCPMarketPlaceServiceImpl.class);

    bind(GcpMarketplaceSignUpHandler.class).annotatedWith(NewSignUp.class).to(NewUserRegistrationHandler.class);
    bind(GcpMarketplaceSignUpHandler.class)
        .annotatedWith(ReturningUser.class)
        .to(ExistingUserRegistrationHandler.class);
  }
}
