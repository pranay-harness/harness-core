package io.harness.ng.core.acl;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.ng.core.user.services.api.NgUserService;

import com.google.inject.AbstractModule;

public class ACLUserModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ACLUserService.class).to(ACLUserServiceImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(NgUserService.class);
    requireBinding(AccessControlAdminClient.class);
  }
}
