package io.harness.ng.core.acl.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.services.api.NgUserService;

import com.google.inject.AbstractModule;

@OwnedBy(PL)
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
