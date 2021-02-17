package io.harness.accesscontrol.management.jobs;

import io.harness.accesscontrol.permissions.jobs.PermissionsManagementJob;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ManagementJob {
  private final PermissionsManagementJob permissionsManagementJob;

  @Inject
  public ManagementJob(PermissionsManagementJob permissionsManagementJob) {
    this.permissionsManagementJob = permissionsManagementJob;
  }

  public void run() {
    permissionsManagementJob.run();
  }
}
