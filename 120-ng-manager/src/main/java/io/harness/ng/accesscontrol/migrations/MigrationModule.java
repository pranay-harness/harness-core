package io.harness.ng.accesscontrol.migrations;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;

import io.harness.accesscontrol.clients.RoleAssignmentClientModule;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.api.EventConsumer;
import io.harness.ng.accesscontrol.migrations.dao.MigrationDAO;
import io.harness.ng.accesscontrol.migrations.dao.MigrationDAOImpl;
import io.harness.ng.accesscontrol.migrations.events.FeatureFlagEventConsumer;
import io.harness.ng.accesscontrol.migrations.services.MigrationService;
import io.harness.ng.accesscontrol.migrations.services.MigrationServiceImpl;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroup.framework.service.ResourceGroupService;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MigrationModule extends AbstractModule {
  private final MigrationConfiguration migrationConfiguration;
  private final EventsFrameworkConfiguration eventsConfig;
  private final ServiceHttpClientConfig userClientConfig;
  private final String userClientSecret;
  private final ServiceHttpClientConfig roleAssignmentClientConfig;
  private final String roleAssignmentClientSecret;
  private static MigrationModule instance;

  public static MigrationModule getInstance(MigrationConfiguration migrationConfiguration,
      EventsFrameworkConfiguration eventsConfig, ServiceHttpClientConfig userClientConfig, String userClientSecret,
      ServiceHttpClientConfig roleAssignmentClientConfig, String roleAssignmentClientSecret) {
    if (instance == null) {
      instance = new MigrationModule(migrationConfiguration, eventsConfig, userClientConfig, userClientSecret,
          roleAssignmentClientConfig, roleAssignmentClientSecret);
    }
    return instance;
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    install(RoleAssignmentClientModule.getInstance(
        roleAssignmentClientConfig, roleAssignmentClientSecret, NG_MANAGER.getServiceId()));
    bind(MigrationService.class).to(MigrationServiceImpl.class).in(Scopes.SINGLETON);
    bind(MigrationDAO.class).to(MigrationDAOImpl.class).in(Scopes.SINGLETON);

    Multibinder<EventConsumer> eventConsumers =
        Multibinder.newSetBinder(binder(), EventConsumer.class, Names.named(FEATURE_FLAG_STREAM));
    eventConsumers.addBinding().to(FeatureFlagEventConsumer.class);
  }

  public void registerRequiredBindings() {
    requireBinding(UserClient.class);
    requireBinding(NgUserService.class);
    requireBinding(ProjectService.class);
    requireBinding(OrganizationService.class);
    requireBinding(ResourceGroupService.class);
  }
}
