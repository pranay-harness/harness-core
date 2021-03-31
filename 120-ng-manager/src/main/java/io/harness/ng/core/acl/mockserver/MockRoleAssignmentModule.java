package io.harness.ng.core.acl.mockserver;

import com.google.inject.AbstractModule;

public class MockRoleAssignmentModule extends AbstractModule {
  @Override
  public void configure() {
    bind(MockRoleAssignmentService.class).to(MockRoleAssignmentServiceImpl.class);
  }
}
