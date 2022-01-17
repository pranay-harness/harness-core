/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.platform;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.AccessControlClientConfiguration;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.client.EnforcementClientConfiguration;
import io.harness.platform.audit.AuditServiceConfiguration;
import io.harness.platform.notification.NotificationServiceConfiguration;
import io.harness.reflection.HarnessReflections;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroup.ResourceGroupServiceConfig;
import io.harness.secret.ConfigSecret;
import io.harness.secret.SecretsConfiguration;
import io.harness.threading.ThreadPoolConfig;

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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.Path;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
public class PlatformConfiguration extends Configuration {
  public static final String SERVICE_ID = "platform-microservice";
  public static final String BASE_PACKAGE = "io.harness.platform";
  public static final String PLATFORM_RESOURCE_PACKAGE = "io.harness.platform.remote";
  public static final String NOTIFICATION_RESOURCE_PACKAGE = "io.harness.notification.remote.resources";
  public static final String AUDIT_RESOURCE_PACKAGE = "io.harness.audit.remote";
  public static final String FILTER_RESOURCE_PACKAGE = "io.harness.filter";
  public static final String RESOURCEGROUP_PACKAGE = "io.harness.resourcegroup";
  public static final String ENFORCEMENT_PACKAGE = "io.harness.enforcement.client.resources";

  @Setter
  @JsonProperty("notificationServiceConfig")
  @ConfigSecret
  private NotificationServiceConfiguration notificationServiceConfig;
  @JsonProperty("commonPoolConfig") private ThreadPoolConfig commonPoolConfig;
  @JsonProperty("auditServiceConfig") @ConfigSecret private AuditServiceConfiguration auditServiceConfig;
  @JsonProperty("resourceGroupServiceConfig")
  @ConfigSecret
  private ResourceGroupServiceConfig resoureGroupServiceConfig;

  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("managerClientConfig") private ServiceHttpClientConfig managerServiceConfig;
  @JsonProperty("ngManagerClientConfig") private ServiceHttpClientConfig ngManagerServiceConfig;
  @JsonProperty("rbacServiceConfig") private ServiceHttpClientConfig rbacServiceConfig;
  @JsonProperty("secrets") @ConfigSecret private PlatformSecrets platformSecrets;
  @JsonProperty(value = "enableAuth", defaultValue = "true") private boolean enableAuth;
  @JsonProperty(value = "environment", defaultValue = "dev") private String environment;
  @JsonProperty(value = "accessControlClient")
  @ConfigSecret
  private AccessControlClientConfiguration accessControlClientConfig;
  @JsonProperty("enforcementClientConfiguration") private EnforcementClientConfiguration enforcementClientConfiguration;
  @JsonProperty("secretsConfiguration") private SecretsConfiguration secretsConfiguration;
  @JsonProperty("hostname") String hostname;
  @JsonProperty("basePathPrefix") String basePathPrefix;

  public static final Collection<Class<?>> ALL_HARNESS_RESOURCES = getAllResources();
  public static final Collection<Class<?>> NOTIFICATION_SERVICE_RESOURCES = getNotificationServiceResourceClasses();
  public static final Collection<Class<?>> AUDIT_SERVICE_RESOURCES = getAuditServiceResourceClasses();
  public static final Collection<Class<?>> RESOURCE_GROUP_RESOURCES = getResourceGroupServiceResourceClasses();

  private static Collection<Class<?>> getAllResources() {
    return HarnessReflections.get().getTypesAnnotatedWith(Path.class);
  }

  private static Collection<Class<?>> getNotificationServiceResourceClasses() {
    return ALL_HARNESS_RESOURCES.stream()
        .filter(clazz -> StringUtils.startsWithAny(clazz.getPackage().getName(), NOTIFICATION_RESOURCE_PACKAGE))
        .collect(Collectors.toSet());
  }

  private static Collection<Class<?>> getAuditServiceResourceClasses() {
    return ALL_HARNESS_RESOURCES.stream()
        .filter(clazz
            -> StringUtils.startsWithAny(clazz.getPackage().getName(), AUDIT_RESOURCE_PACKAGE, FILTER_RESOURCE_PACKAGE))
        .collect(Collectors.toSet());
  }

  private static Collection<Class<?>> getResourceGroupServiceResourceClasses() {
    return ALL_HARNESS_RESOURCES.stream()
        .filter(clazz -> StringUtils.startsWithAny(clazz.getPackage().getName(), RESOURCEGROUP_PACKAGE))
        .collect(Collectors.toSet());
  }

  public static Collection<Class<?>> getPlatformServiceCombinedResourceClasses(PlatformConfiguration appConfig) {
    Collection<Class<?>> resources = new HashSet<>(NOTIFICATION_SERVICE_RESOURCES);
    if (appConfig.getAuditServiceConfig().isEnableAuditService()) {
      resources.addAll(AUDIT_SERVICE_RESOURCES);
    }
    if (appConfig.getResoureGroupServiceConfig().isEnableResourceGroup()) {
      resources.addAll(RESOURCE_GROUP_RESOURCES);
    }
    resources.addAll(ALL_HARNESS_RESOURCES.stream()
                         .filter(clazz -> StringUtils.startsWithAny(clazz.getPackage().getName(), ENFORCEMENT_PACKAGE))
                         .collect(Collectors.toSet()));
    return resources;
  }

  public PlatformConfiguration() {
    DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
    defaultServerFactory.setJerseyRootPath("/api");
    defaultServerFactory.setRequestLogFactory(getDefaultlogbackAccessRequestLogFactory());
    super.setServerFactory(defaultServerFactory);
  }

  private RequestLogFactory<?> getDefaultlogbackAccessRequestLogFactory() {
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
