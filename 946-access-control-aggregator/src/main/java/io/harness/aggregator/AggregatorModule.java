package io.harness.aggregator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.principals.usergroups.UserGroupService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.threading.ExecutorModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class AggregatorModule extends AbstractModule {
  private static AggregatorModule instance;
  private final AggregatorConfiguration configuration;
  private final ExecutorService executorService;

  public AggregatorModule(AggregatorConfiguration configuration) {
    this.configuration = configuration;
    this.executorService = Executors.newFixedThreadPool(5);
  }

  public static synchronized AggregatorModule getInstance(AggregatorConfiguration aggregatorConfiguration) {
    if (instance == null) {
      instance = new AggregatorModule(aggregatorConfiguration);
    }
    return instance;
  }

  @Provides
  @Singleton
  public AggregatorConfiguration getConfiguration() {
    return this.configuration;
  }

  @Override
  protected void configure() {
    ExecutorModule.getInstance().setExecutorService(this.executorService);
    install(ExecutorModule.getInstance());
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(ACLService.class);
    requireBinding(RoleService.class);
    requireBinding(UserGroupService.class);
    requireBinding(ResourceGroupService.class);
    requireBinding(ScopeService.class);
  }
}
