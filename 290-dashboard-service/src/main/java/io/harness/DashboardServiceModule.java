package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import dashboards.CDLandingDashboardResourceClient;
import dashboards.CDLandingDashboardResourceClientModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.govern.ProviderModule;
import io.harness.overviewdashboard.dashboardaggregateservice.impl.DashboardAggregateServiceImpl;
import io.harness.overviewdashboard.dashboardaggregateservice.service.DashboardAggregateService;
import io.harness.overviewdashboard.rbac.impl.DashboardRBACServiceImpl;
import io.harness.overviewdashboard.rbac.service.DashboardRBACService;
import io.harness.pipeline.dashboards.PMSLandingDashboardResourceClient;
import io.harness.pipeline.dashboards.PMSLandingDashboardResourceClientFactory;
import io.harness.pipeline.dashboards.PMSLandingDashboardResourceClientModule;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.pipeline.remote.PipelineServiceHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.serializer.DashboardServiceRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.threading.ExecutorModule;

import com.google.inject.AbstractModule;
import io.harness.user.UserClientModule;
import io.harness.user.remote.UserClient;
import io.harness.user.remote.UserHttpClientFactory;
import io.harness.userng.UserNGClientModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

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
    install(UserNGClientModule.getInstance(config.getNgManagerClientConfig(),config.getDashboardSecretsConfig().getNgManagerServiceSecret(),null));
    install(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(DashboardServiceRegistrars.kryoRegistrars).build();
      }   });
    install(CDLandingDashboardResourceClientModule.getInstance(config.getCdServiceClientConfig(),config.getDashboardSecretsConfig().getNgManagerServiceSecret(),null));
    install(PMSLandingDashboardResourceClientModule.getInstance(config.getCdServiceClientConfig(),config.getDashboardSecretsConfig().getNgManagerServiceSecret(),null));

    bind(DashboardAggregateService.class).to(DashboardAggregateServiceImpl.class);
    bind(DashboardRBACService.class).to(DashboardRBACServiceImpl.class);
  }
}
