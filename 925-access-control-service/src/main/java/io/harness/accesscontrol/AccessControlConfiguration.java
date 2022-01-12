/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.AccessControlClientConfiguration;
import io.harness.accesscontrol.commons.events.EventsConfig;
import io.harness.accesscontrol.commons.iterators.AccessControlIteratorsConfig;
import io.harness.accesscontrol.commons.notifications.NotificationConfig;
import io.harness.accesscontrol.principals.serviceaccounts.ServiceAccountClientConfiguration;
import io.harness.accesscontrol.principals.usergroups.UserGroupClientConfiguration;
import io.harness.accesscontrol.principals.users.UserClientConfiguration;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupClientConfiguration;
import io.harness.accesscontrol.scopes.harness.AccountClientConfiguration;
import io.harness.accesscontrol.scopes.harness.OrganizationClientConfiguration;
import io.harness.accesscontrol.scopes.harness.ProjectClientConfiguration;
import io.harness.aggregator.AggregatorConfiguration;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.client.EnforcementClientConfiguration;
import io.harness.ff.FeatureFlagClientConfiguration;
import io.harness.lock.DistributedLockImplementation;
import io.harness.mongo.MongoConfig;
import io.harness.outbox.OutboxPollConfiguration;
import io.harness.redis.RedisConfig;
import io.harness.remote.client.ServiceHttpClientConfig;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.RequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.Path;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.reflections.Reflections;

@OwnedBy(PL)
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessControlConfiguration extends Configuration {
  public static final String SERVICE_ID = "access-control-microservice";
  public static final String PERMISSION_PACKAGE = "io.harness.accesscontrol.permissions.api";
  public static final String ROLES_PACKAGE = "io.harness.accesscontrol.roles.api";
  public static final String ROLE_ASSIGNMENTS_PACKAGE = "io.harness.accesscontrol.roleassignments.api";
  public static final String ACL_PACKAGE = "io.harness.accesscontrol.acl.api";
  public static final String ACCESSCONTROL_PREFERENCE_PACKAGE = "io.harness.accesscontrol.preference.api";
  public static final String AGGREGATOR_PACKAGE = "io.harness.accesscontrol.aggregator.api";
  public static final String HEALTH_PACKAGE = "io.harness.accesscontrol.health";
  public static final String ENFORCEMENT_PACKAGE = "io.harness.enforcement.client.resources";

  @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("allowedOrigins") private final List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("eventsConfig") private EventsConfig eventsConfig;
  @JsonProperty("redisLockConfig") private RedisConfig redisLockConfig;
  @JsonProperty("iteratorsConfig") private AccessControlIteratorsConfig iteratorsConfig;
  @JsonProperty("accessControlClient") private AccessControlClientConfiguration accessControlClientConfiguration;
  @JsonProperty("resourceGroupClient") private ResourceGroupClientConfiguration resourceGroupClientConfiguration;
  @JsonProperty("userClient") private UserClientConfiguration userClientConfiguration;
  @JsonProperty("userGroupClient") private UserGroupClientConfiguration userGroupClientConfiguration;
  @JsonProperty("projectClient") private ProjectClientConfiguration projectClientConfiguration;
  @JsonProperty("organizationClient") private OrganizationClientConfiguration organizationClientConfiguration;
  @JsonProperty("accountClient") private AccountClientConfiguration accountClientConfiguration;
  @JsonProperty("notificationConfig") private NotificationConfig notificationConfig;
  @JsonProperty("aggregatorModuleConfig") private AggregatorConfiguration aggregatorConfiguration;
  @JsonProperty("enableAuth") @Getter(AccessLevel.NONE) private boolean enableAuth;
  @JsonProperty("defaultServiceSecret") private String defaultServiceSecret;
  @JsonProperty("jwtAuthSecret") private String jwtAuthSecret;
  @JsonProperty("identityServiceSecret") private String identityServiceSecret;
  @JsonProperty("enableAudit") private boolean enableAudit;
  @JsonProperty("auditClientConfig") private ServiceHttpClientConfig auditClientConfig;
  @JsonProperty("featureFlagClientConfiguration") private FeatureFlagClientConfiguration featureFlagClientConfiguration;
  @JsonProperty("outboxPollConfig") private OutboxPollConfiguration outboxPollConfig;
  @JsonProperty("distributedLockImplementation") private DistributedLockImplementation distributedLockImplementation;
  @JsonProperty("serviceAccountClient") private ServiceAccountClientConfiguration serviceAccountClientConfiguration;
  @JsonProperty("enforcementClientConfiguration") private EnforcementClientConfiguration enforcementClientConfiguration;
  @JsonProperty("hostname") private String hostname;
  @JsonProperty("basePathPrefix") private String basePathPrefix;

  public boolean isAuthEnabled() {
    return this.enableAuth;
  }

  public AccessControlConfiguration() {
    DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
    defaultServerFactory.setJerseyRootPath("/api");
    defaultServerFactory.setRequestLogFactory(getDefaultlogbackAccessRequestLogFactory());
    super.setServerFactory(defaultServerFactory);
  }

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(PERMISSION_PACKAGE, ROLES_PACKAGE, ROLE_ASSIGNMENTS_PACKAGE, ACL_PACKAGE,
        ACCESSCONTROL_PREFERENCE_PACKAGE, AGGREGATOR_PACKAGE, HEALTH_PACKAGE, ENFORCEMENT_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  private RequestLogFactory getDefaultlogbackAccessRequestLogFactory() {
    LogbackAccessRequestLogFactory logbackAccessRequestLogFactory = new LogbackAccessRequestLogFactory();
    FileAppenderFactory<IAccessEvent> fileAppenderFactory = new FileAppenderFactory<>();
    fileAppenderFactory.setArchive(true);
    fileAppenderFactory.setCurrentLogFilename("access.log");
    fileAppenderFactory.setThreshold(Level.ALL.toString());
    fileAppenderFactory.setArchivedLogFilenamePattern("access.%d.log.gz");
    fileAppenderFactory.setArchivedFileCount(14);
    logbackAccessRequestLogFactory.setAppenders(ImmutableList.of(fileAppenderFactory));
    return logbackAccessRequestLogFactory;
  }
}
