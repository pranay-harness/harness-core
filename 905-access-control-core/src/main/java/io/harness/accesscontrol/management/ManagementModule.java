package io.harness.accesscontrol.management;

import io.harness.accesscontrol.permissions.jobs.PermissionsManagementJob;

import com.google.inject.AbstractModule;

public class ManagementModule extends AbstractModule {
  private static ManagementModule instance;

  public static synchronized ManagementModule getInstance() {
    if (instance == null) {
      instance = new ManagementModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(PermissionsManagementJob.class);
  }
}
