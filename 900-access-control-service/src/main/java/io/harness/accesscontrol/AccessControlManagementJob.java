package io.harness.accesscontrol;

import io.harness.accesscontrol.permissions.PermissionsManagementJob;
import io.harness.accesscontrol.resources.ResourceTypeManagementJob;
import io.harness.accesscontrol.roles.RolesManagementJob;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AccessControlManagementJob {
  private final ResourceTypeManagementJob resourceTypeManagementJob;
  private final PermissionsManagementJob permissionsManagementJob;
  private final RolesManagementJob rolesManagementJob;

  @Inject
  public AccessControlManagementJob(ResourceTypeManagementJob resourceTypeManagementJob,
      PermissionsManagementJob permissionsManagementJob, RolesManagementJob rolesManagementJob) {
    this.resourceTypeManagementJob = resourceTypeManagementJob;
    this.permissionsManagementJob = permissionsManagementJob;
    this.rolesManagementJob = rolesManagementJob;
  }

  public void run() {
    resourceTypeManagementJob.run();
    permissionsManagementJob.run();
    rolesManagementJob.run();
  }
}
