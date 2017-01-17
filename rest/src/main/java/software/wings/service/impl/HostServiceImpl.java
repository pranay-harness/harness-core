package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.History.Builder.aHistory;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.ResponseMessage.Builder.aResponseMessage;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.common.NotificationMessageResolver.HOST_DELETE_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCodes;
import software.wings.beans.EventType;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HistoryService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.HostCsvFileHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/9/16.
 */
@ValidateOnExecution
@Singleton
public class HostServiceImpl implements HostService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostCsvFileHelper csvFileHelper;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private SettingsService settingsService;
  @Inject private NotificationService notificationService;
  @Inject private EnvironmentService environmentService;
  @Inject private HistoryService historyService;
  @Inject private ConfigService configService;
  @Inject private ExecutorService executorService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Host> list(PageRequest<Host> req) {
    return wingsPersistence.query(Host.class, req);
  }

  @Override
  public Host get(String appId, String envId, String hostId) {
    Host applicationHost = wingsPersistence.createQuery(Host.class)
                               .field(ID_KEY)
                               .equal(hostId)
                               .field("envId")
                               .equal(envId)
                               .field("appId")
                               .equal(appId)
                               .get();
    notNullCheck("Host", applicationHost);
    return applicationHost;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#update(software.wings.beans.infrastructure.Host)
   */
  @Override
  public Host update(String envId, Host host) {
    Host savedHost = get(host.getAppId(), envId, host.getUuid());

    if (savedHost == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Host doesn't exist");
    }

    ImmutableMap.Builder builder = ImmutableMap.<String, Object>builder().put("hostConnAttr", host.getHostConnAttr());
    if (host.getBastionConnAttr() != null) {
      builder.put("bastionConnAttr", host.getBastionConnAttr());
    }
    wingsPersistence.updateFields(Host.class, savedHost.getUuid(), builder.build());

    Host appHost = get(savedHost.getAppId(), savedHost.getEnvId(), host.getUuid());
    return appHost;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#bulkSave(software.wings.beans.infrastructure.Host, java.util.List)
   */
  @Override
  public ResponseMessage bulkSave(String envId, Host baseHost) {
    /*
    Set<String> hostNames = baseHost.getHostNames().stream().filter(hostName ->
    !isNullOrEmpty(hostName)).map(String::trim).collect(Collectors.toSet()); Infrastructure infrastructure =
    infraService.get(infraId); List<Host> applicationHosts = saveHosts(envId, baseHost, hostNames, infrastructure);

    notificationService.sendNotificationAsync(anInformationNotification().withAppId(baseHost.getAppId()).withDisplayText(
        getDecoratedNotificationMessage(ADD_HOST_NOTIFICATION,
            ImmutableMap.of("COUNT", Integer.toString(hostNames.size()), "ENV_NAME",
    environmentService.get(baseHost.getAppId(), envId, false).getName()))) .build());
    //TODO: history entry for bulk save
    */

    return aResponseMessage().build();
  }

  private Host getOrCreateInfraHost(Host baseHost) {
    Host host = wingsPersistence.createQuery(Host.class).field("hostName").equal(baseHost.getHostName()).get();
    if (host == null) {
      SettingAttribute bastionConnAttr = validateAndFetchBastionHostConnectionReference(baseHost.getBastionConnAttr());
      if (bastionConnAttr != null) {
        baseHost.setBastionConnAttr(bastionConnAttr.getUuid());
      }
      host = wingsPersistence.saveAndGet(Host.class, baseHost);
    }
    return host;
  }

  private List<Host> saveHosts(String envId, Host baseHost, Set<String> hostNames, Infrastructure infrastructure) {
    List<Host> applicationHosts = new ArrayList<>();

    hostNames.forEach(hostName -> {
      Host host = aHost()
                      .withHostName(hostName)
                      .withAppId(infrastructure.getAppId())
                      .withHostConnAttr(baseHost.getHostConnAttr())
                      .build();
      host = getOrCreateInfraHost(host);
      Host applicationHost =
          saveHost(aHost().withAppId(baseHost.getAppId()).withEnvId(envId).withHostName(host.getHostName()).build());
      applicationHosts.add(applicationHost);
    });
    return applicationHosts;
  }

  @Override
  public Host saveHost(Host appHost) {
    Host applicationHost = wingsPersistence.createQuery(Host.class)
                               .field("hostName")
                               .equal(appHost.getHostName())
                               .field("appId")
                               .equal(appHost.getAppId())
                               .field("envId")
                               .equal(appHost.getEnvId())
                               .field("serviceTemplateId")
                               .equal(appHost.getServiceTemplateId())
                               .get();
    if (applicationHost == null) {
      applicationHost = wingsPersistence.saveAndGet(Host.class, appHost);
    }
    return applicationHost;
  }

  @Override
  public boolean exist(String appId, String hostId) {
    return wingsPersistence.createQuery(Host.class).field(ID_KEY).equal(hostId).field("appId").equal(appId).getKey()
        != null;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#importHosts(java.lang.String, java.lang.String,
   * software.wings.utils.BoundedInputStream)
   */
  @Override
  public int importHosts(String appId, String envId, String infraId, BoundedInputStream inputStream) {
    List<Host> hosts = csvFileHelper.parseHosts(infraId, appId, envId, inputStream);
    return (int) hosts.stream().map(host -> bulkSave(envId, host)).filter(Objects::nonNull).count();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#getHostsByHostIds(java.lang.String, java.util.List)
   */
  @Override
  public List<Host> getHostsByHostIds(String appId, String envId, List<String> hostUuids) {
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field(ID_KEY)
        .hasAnyOf(hostUuids)
        .asList();
  }

  @Override
  public List<Host> getHostsByEnv(String appId, String envId) {
    return wingsPersistence.createQuery(Host.class).field("appId").equal(appId).field("envId").equal(envId).asList();
  }

  @Override
  public Host getHostByEnv(String appId, String envId, String hostId) {
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field(ID_KEY)
        .equal(hostId)
        .get();
  }

  @Override
  public void delete(String appId, String envId, String hostId) {
    Host applicationHost = get(appId, envId, hostId);
    if (delete(applicationHost)) {
      Environment environment = environmentService.get(applicationHost.getAppId(), applicationHost.getEnvId(), false);
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(applicationHost.getAppId())
              .withDisplayText(getDecoratedNotificationMessage(HOST_DELETE_NOTIFICATION,
                  ImmutableMap.of("HOST_NAME", applicationHost.getHostName(), "ENV_NAME", environment.getName())))
              .build());
      historyService.createAsync(aHistory()
                                     .withEventType(EventType.DELETED)
                                     .withAppId(applicationHost.getAppId())
                                     .withEntityType(EntityType.HOST)
                                     .withEntityId(applicationHost.getUuid())
                                     .withEntityName(applicationHost.getHostName())
                                     .withEntityNewValue(applicationHost)
                                     .withShortDescription("Host " + applicationHost.getHostName() + " deleted")
                                     .withTitle("Host " + applicationHost.getHostName() + " deleted")
                                     .build());
    }
  }

  private boolean delete(Host applicationHost) {
    if (applicationHost != null) {
      boolean delete = wingsPersistence.delete(applicationHost);
      if (delete) {
        executorService.submit(
            () -> configService.deleteByEntityId(applicationHost.getAppId(), applicationHost.getUuid()));
      }
      return delete;
    }
    return false;
  }

  @Override
  public void deleteByEnvironment(String appId, String envId) {
    wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .asList()
        .forEach(this ::delete);
  }

  private List<ServiceTemplate> validateAndFetchServiceTemplate(String appId, List<ServiceTemplate> serviceTemplates) {
    List<ServiceTemplate> fetchedServiceTemplate = new ArrayList<>();
    if (serviceTemplates != null) {
      serviceTemplates.stream()
          .filter(this ::isValidDbReference)
          .map(serviceTemplate -> serviceTemplateService.get(appId, serviceTemplate.getUuid()))
          .forEach(serviceTemplate -> {
            if (serviceTemplate == null) {
              throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "service mapping");
            }
            fetchedServiceTemplate.add(serviceTemplate);
          });
    }
    return fetchedServiceTemplate;
  }

  private SettingAttribute validateAndFetchBastionHostConnectionReference(String settingAttribute) {
    if (isBlank(settingAttribute)) {
      return null;
    }
    SettingAttribute fetchedAttribute = settingsService.get(settingAttribute);
    if (fetchedAttribute == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "bastionConnAttr");
    }
    return fetchedAttribute;
  }

  private boolean isValidDbReference(Base base) {
    return base != null && !isNullOrEmpty(base.getUuid());
  }
}
