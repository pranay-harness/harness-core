package io.harness.accesscontrol;

import io.harness.accesscontrol.permissions.PermissionsManagementJob;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AccessControlManagementJob {
  private final PermissionsManagementJob permissionsManagementJob;

  @Inject
  public AccessControlManagementJob(PermissionsManagementJob permissionsManagementJob) {
    this.permissionsManagementJob = permissionsManagementJob;
  }

  public void run() {
    permissionsManagementJob.run();
  }
}
