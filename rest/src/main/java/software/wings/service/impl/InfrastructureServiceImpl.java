package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.InfrastructureMappingRule.Builder.anInfrastructureMappingRule;
import static software.wings.beans.InfrastructureMappingRule.HostRuleOperator.CONTAINS;
import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.beans.infrastructure.ApplicationHost.Builder.anApplicationHost;
import static software.wings.beans.infrastructure.HostUsage.Builder.aHostUsage;
import static software.wings.common.NotificationMessageResolver.ADD_INFRA_HOST_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCodes;
import software.wings.beans.InfrastructureMappingRule;
import software.wings.beans.InfrastructureMappingRule.Rule;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.beans.infrastructure.ApplicationHostUsage;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.HostUsage;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.beans.infrastructure.Infrastructure.InfrastructureType;
import software.wings.beans.infrastructure.InfrastructureProviderConfig;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.InfrastructureService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class InfrastructureServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class InfrastructureServiceImpl implements InfrastructureService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostService hostService;
  @Inject private ExecutorService executorService;
  @Inject private AppService appService;
  @Inject private SettingsService settingsService;
  @Inject private NotificationService notificationService;
  @Inject private EnvironmentService environmentService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Set<InfrastructureProvider> infrastructureProviders;

  @Inject
  public InfrastructureServiceImpl(Set<InfrastructureProvider> infrastructureProviders) {
    this.infrastructureProviders = infrastructureProviders;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfrastructureService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Infrastructure> list(PageRequest<Infrastructure> req, boolean overview) {
    PageResponse<Infrastructure> infrastructures = wingsPersistence.query(Infrastructure.class, req);
    if (overview) {
      infrastructures.getResponse().forEach(
          infrastructure -> infrastructure.setHostUsage(getInfrastructureHostUsage(infrastructure)));
    }
    return infrastructures;
  }

  private HostUsage getInfrastructureHostUsage(Infrastructure infrastructure) {
    int totalCount = hostService.getInfraHostCount(infrastructure.getUuid());
    int unmappedHostCount = totalCount - hostService.getMappedInfraHostCount(infrastructure.getUuid());
    List<ApplicationHostUsage> applicationHostUsages =
        hostService.getInfrastructureHostUsageByApplication(infrastructure.getUuid());
    return aHostUsage()
        .withTotalCount(totalCount)
        .withUnmappedHostCount(unmappedHostCount)
        .withApplicationHosts(applicationHostUsages)
        .build();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.InfrastructureService#save(software.wings.beans.infrastructure.Infrastructure)
   */
  @Override
  public Infrastructure save(Infrastructure infrastructure) {
    return wingsPersistence.saveAndGet(Infrastructure.class, infrastructure);
  }

  @Override
  public void delete(String infraId) {
    boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(Infrastructure.class)
                                                  .field("appId")
                                                  .equal(Base.GLOBAL_APP_ID)
                                                  .field(ID_KEY)
                                                  .equal(infraId));
    if (deleted) {
      executorService.submit(() -> hostService.deleteByInfra(infraId));
    }
  }

  @Override
  public Infrastructure getInfraByEnvId(String appId, String envId) {
    // TODO:: INFRA
    return wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(GLOBAL_APP_ID).get();
  }

  @Override
  public void createDefaultInfrastructure(String appId) {
    List<Infrastructure> infrastructures =
        wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(GLOBAL_APP_ID).asList();
    if (infrastructures.size() == 0) {
      wingsPersistence.save(
          Infrastructure.Builder.anInfrastructure()
              .withAppId(GLOBAL_APP_ID)
              .withType(InfrastructureType.STATIC)
              .withInfrastructureMappingRules(asList(anInfrastructureMappingRule()
                                                         .withAppId("ALL")
                                                         .withRules(asList(new Rule("HOST_NAME", CONTAINS, "aws")))
                                                         .build()))
              .withName("Static Infrastructure")
              .build());
    }
  }

  @Override
  public String getDefaultInfrastructureId() {
    List<Infrastructure> infrastructures =
        wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(GLOBAL_APP_ID).asList();
    if (infrastructures.size() == 0) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST);
    }
    return infrastructures.get(0).getUuid();
  }

  @Override
  public Infrastructure get(String infraId) {
    Infrastructure infrastructure = wingsPersistence.createQuery(Infrastructure.class)
                                        .field("appId")
                                        .equal(Base.GLOBAL_APP_ID)
                                        .field(ID_KEY)
                                        .equal(infraId)
                                        .get();
    Validator.notNullCheck("Infrastructure", infrastructure);
    return infrastructure;
  }

  @Override
  public PageRequest<Host> listInfraHost(PageRequest<Host> pageRequest) {
    return wingsPersistence.query(Host.class, pageRequest);
  }

  @Override
  public void applyApplicableMappingRules(Infrastructure infrastructure, Host host) {
    List<InfrastructureMappingRule> infrastructureMappingRules = infrastructure.getInfrastructureMappingRules();
    infrastructureMappingRules.forEach(rule -> applyInfraMapping(rule, host));
  }

  @Override
  public void sync(String infraId) {
    Infrastructure infrastructure = get(infraId);
    Validator.notNullCheck("Infrastructure", infrastructure);

    Optional<InfrastructureProvider> infrastructureProvider =
        infrastructureProviders.stream()
            .filter(infraProvider -> infraProvider.infraTypeSupported(infrastructure.getType()))
            .findFirst();

    if (!infrastructureProvider.isPresent()) {
      logger.error("Infra sync failed. No infrastructure provider found for infraId: {} of type : {}",
          infrastructure.getUuid(), infrastructure.getType());
    }

    InfrastructureProviderConfig config = getInfrastructureProviderConfig(infrastructure);
    List<Host> allHost = infrastructureProvider.get().getAllHost(config);

    List<String> deletedProviderHosts = getDeletedHostNames(infrastructure, allHost);
    deletedProviderHosts.forEach(hostName -> deleteInfraHostByName(infrastructure, hostName));

    List<Host> newProviderHosts = getNewProviderHosts(infrastructure, allHost);
    SettingAttribute connectionAttributes = wingsPersistence.createQuery(SettingAttribute.class)
                                                .field("value.type")
                                                .equal(HOST_CONNECTION_ATTRIBUTES)
                                                .get(); // TODO:: remove it

    newProviderHosts.forEach(host -> {
      host.setHostConnAttr(connectionAttributes.getUuid());
      host.setInfraId(infrastructure.getUuid());
      Host savedHost = wingsPersistence.saveAndGet(Host.class, host);
      applyApplicableMappingRules(infrastructure, savedHost);
    });
    if (newProviderHosts.size() > 0) {
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(GLOBAL_APP_ID)
              .withDisplayText(getDecoratedNotificationMessage(ADD_INFRA_HOST_NOTIFICATION,
                  ImmutableMap.of(
                      "COUNT", Integer.toString(newProviderHosts.size()), "INFRA_NAME", infrastructure.getName())))
              .build());
    }
  }

  private void deleteInfraHostByName(Infrastructure infrastructure, String hostName) {
    Host host = wingsPersistence.createQuery(Host.class)
                    .field("infraId")
                    .equal(infrastructure.getUuid())
                    .field("hostName")
                    .equal(hostName)
                    .get();
    wingsPersistence.delete(host);
  }

  private List<Host> getNewProviderHosts(Infrastructure infrastructure, List<Host> allHost) {
    Set<String> existingHostNames = wingsPersistence.createQuery(Host.class)
                                        .field("infraId")
                                        .equal(infrastructure.getUuid())
                                        .retrievedFields(true, "hostName")
                                        .asList()
                                        .stream()
                                        .map(Host::getHostName)
                                        .collect(Collectors.toSet());
    return allHost.stream()
        .filter(host -> !existingHostNames.contains(host.getHostName()))
        .collect(Collectors.toList());
  }

  private List<String> getDeletedHostNames(Infrastructure infrastructure, List<Host> allHost) {
    Set<String> existingHostNames = wingsPersistence.createQuery(Host.class)
                                        .field("infraId")
                                        .equal(infrastructure.getUuid())
                                        .retrievedFields(true, "hostName")
                                        .asList()
                                        .stream()
                                        .map(Host::getHostName)
                                        .collect(Collectors.toSet());
    existingHostNames.removeAll(allHost.stream().map(Host::getHostName).collect(Collectors.toList()));
    return existingHostNames.stream().collect(Collectors.toList());
  }

  public InfrastructureProviderConfig getInfrastructureProviderConfig(Infrastructure infrastructure) {
    InfrastructureProviderConfig config = null;
    if (infrastructure.getInfrastructureConfigId() != null) {
      SettingAttribute settingAttribute = settingsService.get(infrastructure.getInfrastructureConfigId());
      if (settingAttribute == null || !(settingAttribute.getValue() instanceof InfrastructureProviderConfig)) {
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "message",
            "valid InfrastructureProviderConfig could not be found for infraId:" + infrastructure.getUuid());
      }
      config = (InfrastructureProviderConfig) settingAttribute.getValue();
    }
    return config;
  }

  private void applyInfraMapping(InfrastructureMappingRule mappingRule, Host host) {
    Map<String, String> context = getHostAttributes(host);
    if (mappingRule.evaluate(context)) {
      List<Application> applications = getApplicableApplications(mappingRule);
      applications.stream()
          .filter(Objects::nonNull)
          .forEach(application
              -> addApplicationHost(application.getAppId(), mappingRule.getEnvId(), mappingRule.getTagId(), host));
    }
  }

  public void addApplicationHost(String appId, String envId, String tagId, Host host) {
    List<Environment> environments = environmentService.getEnvByApp(appId);
    environments.stream()
        .filter(environment -> envId == null || envId.equals(environment.getUuid()))
        .forEach(environment -> {
          ApplicationHost appHost = anApplicationHost()
                                        .withAppId(environment.getAppId())
                                        .withEnvId(environment.getUuid())
                                        .withInfraId(host.getInfraId())
                                        .withHostName(host.getHostName())
                                        .withHost(host)
                                        .build();
          hostService.saveApplicationHost(appHost, tagId);
          // TODO: add notification
        });
  }

  private List<Application> getApplicableApplications(InfrastructureMappingRule mappingRule) {
    return "ALL".equals(mappingRule.getAppId()) ? appService.list(aPageRequest().build(), false, 0, 0).getResponse()
                                                : asList(appService.get(mappingRule.getAppId()));
  }

  private Map<String, String> getHostAttributes(Host host) {
    return ImmutableMap.of("HOST_NAME", host.getHostName());
  }
}
