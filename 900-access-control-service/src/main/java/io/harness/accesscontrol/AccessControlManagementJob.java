package io.harness.accesscontrol;

import io.harness.accesscontrol.permissions.PermissionsManagementJob;
import io.harness.accesscontrol.roles.RolesManagementJob;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AccessControlManagementJob {
  private final PermissionsManagementJob permissionsManagementJob;
  private final RolesManagementJob rolesManagementJob;

  @Inject
  public AccessControlManagementJob(
      PermissionsManagementJob permissionsManagementJob, RolesManagementJob rolesManagementJob) {
    this.permissionsManagementJob = permissionsManagementJob;
    this.rolesManagementJob = rolesManagementJob;
  }

  public void run() {
    permissionsManagementJob.run();
    rolesManagementJob.run();
  }
}
