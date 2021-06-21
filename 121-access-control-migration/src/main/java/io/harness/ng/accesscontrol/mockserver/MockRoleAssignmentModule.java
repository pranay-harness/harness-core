package io.harness.ng.accesscontrol.mockserver;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.mockserver.services.MockRoleAssignmentService;
import io.harness.ng.accesscontrol.mockserver.services.MockRoleAssignmentServiceImpl;

import com.google.inject.AbstractModule;

@OwnedBy(PL)
public class MockRoleAssignmentModule extends AbstractModule {
  @Override
  public void configure() {
    bind(MockRoleAssignmentService.class).to(MockRoleAssignmentServiceImpl.class);
  }
}
