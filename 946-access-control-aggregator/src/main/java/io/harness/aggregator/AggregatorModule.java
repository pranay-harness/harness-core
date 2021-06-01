package io.harness.aggregator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.principals.usergroups.UserGroupService;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBO;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.aggregator.consumers.ChangeConsumer;
import io.harness.aggregator.consumers.ResourceGroupChangeConsumerImpl;
import io.harness.aggregator.consumers.RoleAssignmentChangeConsumerImpl;
import io.harness.aggregator.consumers.RoleChangeConsumerImpl;
import io.harness.aggregator.consumers.UserGroupChangeConsumerImpl;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
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
    bind(new TypeLiteral<ChangeConsumer<RoleAssignmentDBO>>() {
    }).to(new TypeLiteral<RoleAssignmentChangeConsumerImpl>() {
      }).in(Scopes.SINGLETON);

    bind(new TypeLiteral<ChangeConsumer<ResourceGroupDBO>>() {
    }).to(new TypeLiteral<ResourceGroupChangeConsumerImpl>() {
      }).in(Scopes.SINGLETON);

    bind(new TypeLiteral<ChangeConsumer<UserGroupDBO>>() {
    }).to(new TypeLiteral<UserGroupChangeConsumerImpl>() {
      }).in(Scopes.SINGLETON);

    bind(new TypeLiteral<ChangeConsumer<RoleDBO>>() {
    }).to(new TypeLiteral<RoleChangeConsumerImpl>() {
      }).in(Scopes.SINGLETON);

    bind(AggregatorMetricsService.class).to(AggregatorMetricsServiceImpl.class).in(Scopes.SINGLETON);
  }

  private void registerRequiredBindings() {
    requireBinding(ACLService.class);
    requireBinding(RoleService.class);
    requireBinding(UserGroupService.class);
    requireBinding(ResourceGroupService.class);
    requireBinding(ScopeService.class);
  }
}
