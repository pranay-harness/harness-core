/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.accesscontrol.mockserver;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(PL)
public class MockRoleAssignmentModule extends AbstractModule {
  @Override
  public void configure() {
    bind(MockRoleAssignmentService.class).to(MockRoleAssignmentServiceImpl.class);
  }
}
