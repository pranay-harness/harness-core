package io.harness;

import static io.harness.AuthorizationServiceHeader.DASHBOAD_AGGREGATION_SERVICE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.govern.ProviderModule;
import io.harness.organization.OrganizationClientModule;
import io.harness.overviewdashboard.dashboardaggregateservice.impl.OverviewDashboardServiceImpl;
import io.harness.overviewdashboard.dashboardaggregateservice.service.OverviewDashboardService;
import io.harness.overviewdashboard.rbac.impl.DashboardRBACServiceImpl;
import io.harness.overviewdashboard.rbac.service.DashboardRBACService;
import io.harness.pipeline.dashboards.PMSLandingDashboardResourceClientModule;
import io.harness.serializer.DashboardServiceRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.threading.ExecutorModule;
import io.harness.token.TokenClientModule;
import io.harness.userng.UserNGClientModule;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dashboards.CDLandingDashboardResourceClientModule;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class DashboardServiceModule extends AbstractModule {
  private final DashboardServiceConfig config;

  public DashboardServiceModule(DashboardServiceConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(ExecutorModule.getInstance());
    install(UserNGClientModule.getInstance(config.getNgManagerClientConfig(),
        config.getDashboardSecretsConfig().getNgManagerServiceSecret(),
        AuthorizationServiceHeader.DASHBOAD_AGGREGATION_SERVICE.getServiceId()));
    install(OrganizationClientModule.getInstance(config.getNgManagerClientConfig(),
        config.getDashboardSecretsConfig().getNgManagerServiceSecret(),
        AuthorizationServiceHeader.DASHBOAD_AGGREGATION_SERVICE.getServiceId()));
    install(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(DashboardServiceRegistrars.kryoRegistrars)
            .build();
      }
    });
    install(CDLandingDashboardResourceClientModule.getInstance(
        config.getCdServiceClientConfig(), config.getDashboardSecretsConfig().getNgManagerServiceSecret(), null));
    install(PMSLandingDashboardResourceClientModule.getInstance(
        config.getCdServiceClientConfig(), config.getDashboardSecretsConfig().getNgManagerServiceSecret(), null));
    install(new TokenClientModule(config.getNgManagerClientConfig(),
        config.getDashboardSecretsConfig().getNgManagerServiceSecret(), DASHBOAD_AGGREGATION_SERVICE.getServiceId()));
    bind(OverviewDashboardService.class).to(OverviewDashboardServiceImpl.class);
    bind(DashboardRBACService.class).to(DashboardRBACServiceImpl.class);
  }
}
