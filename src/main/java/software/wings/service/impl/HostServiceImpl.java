package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.History.Builder.aHistory;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.ResponseMessage.Builder.aResponseMessage;
import static software.wings.beans.ResponseMessage.ResponseTypeEnum.WARN;
import static software.wings.common.NotificationMessageResolver.ADD_HOST_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.HOST_DELETE_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import com.mongodb.DuplicateKeyException;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCodes;
import software.wings.beans.EventType;
import software.wings.beans.Host;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.beans.Tag.TagType;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HistoryService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TagService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.HostCsvFileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
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
  @Inject private InfrastructureService infraService;
  @Inject private SettingsService settingsService;
  @Inject private TagService tagService;
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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Host get(String infraId, String hostId) {
    return wingsPersistence.createQuery(Host.class)
        .field(ID_KEY)
        .equal(hostId)
        .field("infraId")
        .equal(infraId)
        .field("appId")
        .equal(Base.GLOBAL_APP_ID)
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#save(software.wings.beans.Host)
   */
  private Host save(Host host) {
    host.setHostName(host.getHostName().trim());
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#update(software.wings.beans.Host)
   */
  @Override
  public Host update(String envId, Host host) {
    Host savedHost = get(host.getInfraId(), host.getUuid());

    ImmutableMap.Builder builder = ImmutableMap.<String, Object>builder()
                                       .put("hostName", host.getHostName())
                                       .put("hostConnAttr", host.getHostConnAttr());
    if (host.getBastionConnAttr() != null) {
      builder.put("bastionConnAttr", host.getBastionConnAttr());
    }
    wingsPersistence.updateFields(Host.class, host.getUuid(), builder.build());

    Tag tag = validateAndFetchTag(host.getAppId(), envId, host.getConfigTag());

    if (tag != null && tag.equals(savedHost.getConfigTag())) {
      return wingsPersistence.get(Host.class, host.getAppId(), host.getUuid());
    }

    // Tag update -> should update host mapped in template

    if (tag == null) {
      tag = tagService.getDefaultTagForUntaggedHosts(host.getAppId(), envId);
    }

    List<Host> hostsByTags = getHostsByTags(host.getAppId(), envId, asList(tag));
    hostsByTags.add(host);
    tagService.tagHosts(tag, hostsByTags);

    List<ServiceTemplate> serviceTemplates =
        validateAndFetchServiceTemplate(host.getAppId(), host.getServiceTemplates());
    serviceTemplates.forEach(serviceTemplate
        -> serviceTemplateService.addHosts(serviceTemplate, asList(get(host.getInfraId(), host.getUuid()))));
    return host;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#importHosts(java.lang.String, java.lang.String,
   * software.wings.utils.BoundedInputStream)
   */
  @Override
  public int importHosts(String appId, String envId, BoundedInputStream inputStream) {
    //    Infra infra = infraService.getInfraByEnvId(appId, envId);
    //    notNullCheck("infra", infra);
    //    List<Host> hosts = csvFileHelper.parseHosts(infra, inputStream);
    //    List<String> Ids = wingsPersistence.save(hosts);
    //    return Ids.size();
    // TODO:: INFRA:
    return 0;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#getHostsByHostIds(java.lang.String, java.util.List)
   */
  @Override
  public List<Host> getHostsByHostIds(String appId, String infraId, List<String> hostUuids) {
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("infraId")
        .equal(infraId)
        .field(ID_KEY)
        .hasAnyOf(hostUuids)
        .asList();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#getHostsByTags(java.lang.String, java.util.List)
   */
  @Override
  public List<Host> getHostsByTags(String appId, String envId, List<Tag> tags) {
    // TODO:: INFRA:
    String infraId = infraService.getInfraByEnvId(envId).getUuid();
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("infraId")
        .equal(infraId)
        .field("configTag")
        .hasAnyOf(tags)
        .asList();
  }

  @Override
  public void setTag(Host host, Tag tag) {
    if (tag == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "Can not tag host with null tag");
    }
    UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).set("configTag", tag);
    wingsPersistence.update(host, updateOp);
  }

  @Override
  public void removeTagFromHost(Host host, Tag tag) {
    if (!host.getConfigTag().getTagType().equals(TagType.UNTAGGED_HOST)) {
      setTag(host, tagService.getDefaultTagForUntaggedHosts(tag.getAppId(), tag.getEnvId()));
    }
  }

  @Override
  public List<Host> getHostsByEnv(String appId, String envId) {
    // TODO:: INFRA:
    String infraId = infraService.getInfraByEnvId(envId).getUuid();
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("infraId")
        .equal(infraId)
        .asList();
  }

  @Override
  public Host getHostByEnv(String appId, String envId, String hostId) {
    // TODO:: INFRA:
    String infraId = infraService.getInfraByEnvId(envId).getUuid();
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("infraId")
        .equal(infraId)
        .field(ID_KEY)
        .equal(hostId)
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#exportHosts(java.lang.String, java.lang.String)
   */
  @Override
  public File exportHosts(String appId, String infraId) {
    // TODO:: INFRA:
    List<Host> hosts = wingsPersistence.createQuery(Host.class).field("infraId").equal(infraId).asList();
    return csvFileHelper.createHostsFile(hosts);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#delete(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public void delete(String appId, String infraId, String envId, String hostId) {
    Host host = get(infraId, hostId);
    if (delete(host)) {
      Environment environment = environmentService.get(host.getAppId(), envId, false);
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(host.getAppId())
              .withDisplayText(getDecoratedNotificationMessage(HOST_DELETE_NOTIFICATION,
                  ImmutableMap.of("HOST_NAME", host.getHostName(), "ENV_NAME", environment.getName())))
              .build());
      historyService.createAsync(aHistory()
                                     .withEventType(EventType.CREATED)
                                     .withAppId(host.getAppId())
                                     .withEntityType(EntityType.HOST)
                                     .withEntityId(host.getUuid())
                                     .withEntityName(host.getHostName())
                                     .withEntityNewValue(host)
                                     .withShortDescription("Host " + host.getHostName() + " created")
                                     .withTitle("Host " + host.getHostName() + " created")
                                     .build());
    }
  }

  private boolean delete(Host host) {
    if (host != null) {
      boolean delete = wingsPersistence.delete(host);
      if (delete) {
        serviceTemplateService.deleteHostFromTemplates(host);
        executorService.submit(() -> configService.deleteByEntityId(host.getAppId(), host.getUuid()));
      }
      historyService.createAsync(aHistory()
                                     .withEventType(EventType.DELETED)
                                     .withAppId(host.getAppId())
                                     .withEntityType(EntityType.HOST)
                                     .withEntityId(host.getUuid())
                                     .withEntityName(host.getHostName())
                                     .withEntityNewValue(host)
                                     .withShortDescription("Host " + host.getHostName() + " deleted")
                                     .withTitle("Host " + host.getHostName() + " deleted")
                                     .build());
      return delete;
    }
    return false;
  }

  @Override
  public void deleteByInfra(String infraId) {
    wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(Base.GLOBAL_APP_ID)
        .field("infraId")
        .equal(infraId)
        .asList()
        .forEach(this ::delete);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#bulkSave(software.wings.beans.Host, java.util.List)
   */
  @Override
  public ResponseMessage bulkSave(String infraId, String envId, Host baseHost) {
    List<String> hostNames = baseHost.getHostNames()
                                 .stream()
                                 .filter(hostName -> !isNullOrEmpty(hostName))
                                 .map(String::trim)
                                 .collect(Collectors.toList());
    List<ServiceTemplate> serviceTemplates =
        validateAndFetchServiceTemplate(baseHost.getAppId(), baseHost.getServiceTemplates());
    Environment environment = environmentService.get(baseHost.getAppId(), envId, false);
    Infrastructure infrastructure = infraService.get(infraId);
    List<String> duplicateHostNames = new ArrayList<>();
    List<Host> savedHosts = new ArrayList<>();

    hostNames.forEach(hostName -> {
      Host host = aHost()
                      .withHostName(hostName)
                      .withAppId(infrastructure.getAppId())
                      .withInfraId(infrastructure.getUuid())
                      .withHostConnAttr(baseHost.getHostConnAttr())
                      .withEnvironments(asList(environment))
                      .build();
      SettingAttribute bastionConnAttr =
          validateAndFetchBastionHostConnectionReference(baseHost.getAppId(), baseHost.getBastionConnAttr());
      if (bastionConnAttr != null) {
        host.setBastionConnAttr(bastionConnAttr);
      }
      // TODO:: Infra
      Tag configTag = validateAndFetchTag(baseHost.getAppId(), envId, baseHost.getConfigTag());
      if (configTag == null) {
        configTag = tagService.getDefaultTagForUntaggedHosts(baseHost.getAppId(), envId);
      }
      host.setConfigTag(configTag);
      try {
        Host savedHost = save(host);
        savedHosts.add(savedHost);
      } catch (DuplicateKeyException dupEx) {
        logger.error("Duplicate host insertion for host {} {}", host, dupEx.getMessage());
        duplicateHostNames.add(host.getHostName());
      }
    });
    serviceTemplates.forEach(serviceTemplate -> serviceTemplateService.addHosts(serviceTemplate, savedHosts));

    int countOfNewHostsAdded = hostNames.size() - duplicateHostNames.size();
    if (countOfNewHostsAdded > 0) {
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(baseHost.getAppId())
              .withDisplayText(getDecoratedNotificationMessage(ADD_HOST_NOTIFICATION,
                  ImmutableMap.of("COUNT", Integer.toString(countOfNewHostsAdded), "ENV_NAME", environment.getName())))
              .build());
      // TODO: history entry for bulk save
    }

    return aResponseMessage()
        .withErrorType(WARN)
        .withCode(ErrorCodes.DUPLICATE_HOST_NAMES)
        .withMessage(Joiner.on(",").join(duplicateHostNames))
        .build();
  }

  private List<Environment> validateAndFetchEnvironments(String appId, List<Environment> environments) {
    List<Environment> environmentList = new ArrayList<>();
    if (!(Strings.isNullOrEmpty(appId) || environments == null || environments.size() == 0)) {
      environments.forEach(
          environment -> environmentList.add(environmentService.get(appId, environment.getUuid(), false)));
    }
    return environmentList;
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

  private Tag validateAndFetchTag(String appId, String envId, Tag tag) {
    Tag fetchedTag = null;
    if (isValidDbReference(tag)) {
      fetchedTag = tagService.get(appId, envId, tag.getUuid());
      if (fetchedTag == null) {
        throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "configTags");
      }
    }
    return fetchedTag;
  }

  private SettingAttribute validateAndFetchBastionHostConnectionReference(
      String appId, SettingAttribute settingAttribute) {
    if (!isValidDbReference(settingAttribute)) {
      return null;
    }
    SettingAttribute fetchedAttribute = settingsService.get(appId, settingAttribute.getUuid());
    if (fetchedAttribute == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "bastionConnAttr");
    }
    return fetchedAttribute;
  }

  private boolean isValidDbReference(Base base) {
    return base != null && !isNullOrEmpty(base.getUuid());
  }
}
