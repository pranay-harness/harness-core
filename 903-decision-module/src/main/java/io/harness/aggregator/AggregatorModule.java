package io.harness.aggregator;

import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.aggregator.services.HACLAggregatorServiceImpl;
import io.harness.aggregator.services.apis.ACLAggregatorService;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;

import com.google.inject.AbstractModule;

public class AggregatorModule extends AbstractModule {
  private static AggregatorModule instance;

  public static AggregatorModule getInstance() {
    if (instance == null) {
      instance = new AggregatorModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ACLAggregatorService.class).to(HACLAggregatorServiceImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(RoleService.class);
    requireBinding(RoleAssignmentService.class);
    requireBinding(ACLService.class);
    requireBinding(ResourceGroupClient.class);
  }
}
