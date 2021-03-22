package io.harness.ng.accesscontrol.migrations;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.ng.accesscontrol.migrations.dao.AccessControlMigrationDAO;
import io.harness.ng.accesscontrol.migrations.dao.AccessControlMigrationDAOImpl;
import io.harness.ng.accesscontrol.migrations.events.AccessControlMigrationFeatureFlagEventListener;
import io.harness.ng.accesscontrol.migrations.services.AccessControlMigrationService;
import io.harness.ng.accesscontrol.migrations.services.AccessControlMigrationServiceImpl;
import io.harness.ng.core.event.MessageListener;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.resourcegroup.framework.service.ResourceGroupService;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationModule extends AbstractModule {
  private static AccessControlMigrationModule instance;

  public static synchronized AccessControlMigrationModule getInstance() {
    if (instance == null) {
      instance = new AccessControlMigrationModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(AccessControlMigrationService.class).to(AccessControlMigrationServiceImpl.class).in(Scopes.SINGLETON);
    bind(AccessControlMigrationDAO.class).to(AccessControlMigrationDAOImpl.class).in(Scopes.SINGLETON);
    bind(MessageListener.class)
        .annotatedWith(Names.named("access_control_migration" + EventsFrameworkConstants.FEATURE_FLAG_STREAM))
        .to(AccessControlMigrationFeatureFlagEventListener.class);
  }

  public void registerRequiredBindings() {
    requireBinding(UserClient.class);
    requireBinding(NgUserService.class);
    requireBinding(ProjectService.class);
    requireBinding(OrganizationService.class);
    requireBinding(ResourceGroupService.class);
    requireBinding(AccessControlAdminClient.class);
  }
}
