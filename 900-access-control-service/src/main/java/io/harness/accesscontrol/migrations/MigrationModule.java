package io.harness.accesscontrol.migrations;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.harness.accesscontrol.commons.events.EventConsumer;
import io.harness.accesscontrol.commons.events.EventsConfig;
import io.harness.accesscontrol.roleassignments.RoleAssignmentModule;
import io.harness.accesscontrol.scopes.ScopeModule;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.organizationmanagerclient.OrganizationManagementClientModule;
import io.harness.projectmanagerclient.ProjectManagementClientModule;
import io.harness.remote.client.ServiceHttpClientConfig;
import lombok.AllArgsConstructor;

import java.time.Duration;

import static io.harness.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;

@AllArgsConstructor
public class MigrationModule extends AbstractModule {
  private final MigrationConfiguration migrationConfiguration;
  private final EventsConfig eventsConfig;
  private final ServiceHttpClientConfig userClientConfig;
  private final String userClientSecret;
  private final ServiceHttpClientConfig projectOrgsClientConfig;
  private final String projectOrgsClientSecret;
  private static MigrationModule instance;

  public static MigrationModule getInstance(MigrationConfiguration migrationConfiguration, EventsConfig eventsConfig,
      ServiceHttpClientConfig userClientConfig, String userClientSecret,
      ServiceHttpClientConfig projectOrgsClientConfig, String projectOrgsClientSecret) {
    if (instance == null) {
      instance = new MigrationModule(migrationConfiguration, eventsConfig, userClientConfig, userClientSecret,
          projectOrgsClientConfig, projectOrgsClientSecret);
    }
    return instance;
  }

  @Override
  protected void configure() {
    requireBindings();
    install(RoleAssignmentModule.getInstance());
    install(ScopeModule.getInstance());
    install(new ProjectManagementClientModule(
        projectOrgsClientConfig, projectOrgsClientSecret, ACCESS_CONTROL_SERVICE.getServiceId()));
    install(new OrganizationManagementClientModule(
        projectOrgsClientConfig, projectOrgsClientSecret, ACCESS_CONTROL_SERVICE.getServiceId()));

    Multibinder<EventConsumer> eventConsumers =
        Multibinder.newSetBinder(binder(), EventConsumer.class, Names.named(FEATURE_FLAG_STREAM));
    eventConsumers.addBinding().to(FeatureFlagEventConsumer.class);
  }

  @Provides
  @Named(FEATURE_FLAG_STREAM)
  public Consumer getConsumer() {
    if (!eventsConfig.isEnabled()) {
      return NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME);
    }
    return RedisConsumer.of(FEATURE_FLAG_STREAM, ACCESS_CONTROL_SERVICE.getServiceId(), eventsConfig.getRedisConfig(),
        Duration.ofMinutes(10L), 2);
  }

  public void requireBindings() {
    requireBinding(UserClient.class);
  }
}
