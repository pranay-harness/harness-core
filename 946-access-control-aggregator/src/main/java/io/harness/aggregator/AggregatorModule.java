package io.harness.aggregator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.principals.usergroups.UserGroupService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class AggregatorModule extends AbstractModule {
  private static AggregatorModule instance;
  private final AggregatorConfiguration configuration;

  public AggregatorModule(AggregatorConfiguration configuration) {
    this.configuration = configuration;
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
