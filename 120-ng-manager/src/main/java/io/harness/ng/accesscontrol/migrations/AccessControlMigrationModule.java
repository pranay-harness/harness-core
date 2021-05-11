package io.harness.ng.accesscontrol.migrations;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dao.AccessControlMigrationDAO;
import io.harness.dao.AccessControlMigrationDAOImpl;
import io.harness.events.AccessControlMigrationHandler;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.ng.core.event.MessageListener;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.repositories.AccessControlMigrationRepository;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.services.AccessControlMigrationService;
import io.harness.services.AccessControlMigrationServiceImpl;
import io.harness.user.remote.UserClient;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationModule extends AbstractModule {
  private static io.harness.AccessControlMigrationModule instance;

  public static synchronized io.harness.AccessControlMigrationModule getInstance() {
    if (instance == null) {
      instance = new io.harness.AccessControlMigrationModule();
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
        .to(AccessControlMigrationHandler.class);
  }

  public void registerRequiredBindings() {
    requireBinding(AccessControlMigrationRepository.class);
    requireBinding(UserClient.class);
    requireBinding(NgUserService.class);
    requireBinding(ProjectService.class);
    requireBinding(OrganizationService.class);
    requireBinding(ResourceGroupClient.class);
    requireBinding(AccessControlAdminClient.class);
  }
}
