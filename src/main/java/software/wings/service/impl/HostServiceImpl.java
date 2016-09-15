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
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ApplicationHost;
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
import software.wings.utils.Validator;

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
  public PageResponse<ApplicationHost> list(PageRequest<ApplicationHost> req) {
    return wingsPersistence.query(ApplicationHost.class, req);
  }

  @Override
  public ApplicationHost get(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String hostId) {
    ApplicationHost applicationHost = wingsPersistence.createQuery(ApplicationHost.class)
                                          .field(ID_KEY)
                                          .equal(hostId)
                                          .field("envId")
                                          .equal(envId)
                                          .field("appId")
                                          .equal(appId)
                                          .get();
    Validator.notNullCheck("ApplicationHost", applicationHost);
    return applicationHost;
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
  public ApplicationHost update(String envId, Host host) {
    return null; // TODO:: Infra
    //    ApplicationHost savedHost = get(host.getInfraId(), host.getUuid());
    //
    //    ImmutableMap.Builder builder = ImmutableMap.<String, Object>builder().put("hostName",
    //    host.getHostName()).put("hostConnAttr", host.getHostConnAttr()); if (host.getBastionConnAttr() != null) {
    //      builder.put("bastionConnAttr", host.getBastionConnAttr());
    //    }
    //    wingsPersistence.updateFields(Host.class, host.getUuid(), builder.build());
    //
    //    Tag tag = validateAndFetchTag(host.getAppId(), envId, host.getConfigTag());
    //
    //    if (tag != null && tag.equals(savedHost.getConfigTag())) {
    //      return wingsPersistence.get(Host.class, host.getAppId(), host.getUuid());
    //    }
    //
    //    //Tag update -> should update host mapped in template
    //
    //    if (tag == null) {
    //      tag = tagService.getDefaultTagForUntaggedHosts(host.getAppId(), envId);
    //    }
    //
    //    List<Host> hostsByTags = getHostsByTags(host.getAppId(), envId, asList(tag));
    //    hostsByTags.add(host);
    //    tagService.tagHosts(tag, hostsByTags);
    //
    //    List<ServiceTemplate> serviceTemplates = validateAndFetchServiceTemplate(host.getAppId(),
    //    host.getServiceTemplates()); serviceTemplates.forEach(serviceTemplate ->
    //    serviceTemplateService.addHosts(serviceTemplate, asList(get(host.getInfraId(), host.getUuid())))); return
    //    host;
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
  public List<ApplicationHost> getHostsByHostIds(String appId, String envId, List<String> hostUuids) {
    return wingsPersistence.createQuery(ApplicationHost.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field(ID_KEY)
        .hasAnyOf(hostUuids)
        .asList();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#getHostsByTags(java.lang.String, java.util.List)
   */
  @Override
  public List<ApplicationHost> getHostsByTags(String appId, String envId, List<Tag> tags) {
    return wingsPersistence.createQuery(ApplicationHost.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field("configTag")
        .hasAnyOf(tags)
        .asList();
  }

  @Override
  public void setTag(ApplicationHost host, Tag tag) {
    if (tag == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "Can not tag host with null tag");
    }
    UpdateOperations<ApplicationHost> updateOp =
        wingsPersistence.createUpdateOperations(ApplicationHost.class).set("configTag", tag);
    wingsPersistence.update(host, updateOp);
  }

  @Override
  public void removeTagFromHost(ApplicationHost host, Tag tag) {
    if (!host.getConfigTag().getTagType().equals(TagType.UNTAGGED_HOST)) {
      setTag(host, tagService.getDefaultTagForUntaggedHosts(tag.getAppId(), tag.getEnvId()));
    }
  }

  @Override
  public List<ApplicationHost> getHostsByEnv(String appId, String envId) {
    // TODO:: INFRA:
    String infraId = infraService.getInfraByEnvId(envId).getUuid();
    return wingsPersistence.createQuery(ApplicationHost.class)
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

  @Override
  public void delete(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String hostId) {
    ApplicationHost applicationHost = get(appId, envId, hostId);
    if (delete(applicationHost)) {
      Environment environment = environmentService.get(applicationHost.getAppId(), envId, false);
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(applicationHost.getAppId())
              .withDisplayText(getDecoratedNotificationMessage(HOST_DELETE_NOTIFICATION,
                  ImmutableMap.of(
                      "HOST_NAME", applicationHost.getHost().getHostName(), "ENV_NAME", environment.getName())))
              .build());
      historyService.createAsync(
          aHistory()
              .withEventType(EventType.CREATED)
              .withAppId(applicationHost.getAppId())
              .withEntityType(EntityType.HOST)
              .withEntityId(applicationHost.getUuid())
              .withEntityName(applicationHost.getHost().getHostName())
              .withEntityNewValue(applicationHost)
              .withShortDescription("Host " + applicationHost.getHost().getHostName() + " created")
              .withTitle("Host " + applicationHost.getHost().getHostName() + " created")
              .build());
    }
  }

  private boolean delete(ApplicationHost applicationHost) {
    if (applicationHost != null) {
      boolean delete = wingsPersistence.delete(applicationHost);
      if (delete) {
        serviceTemplateService.deleteHostFromTemplates(applicationHost);
        executorService.submit(
            () -> configService.deleteByEntityId(applicationHost.getAppId(), applicationHost.getUuid()));
      }
      historyService.createAsync(
          aHistory()
              .withEventType(EventType.DELETED)
              .withAppId(applicationHost.getAppId())
              .withEntityType(EntityType.HOST)
              .withEntityId(applicationHost.getUuid())
              .withEntityName(applicationHost.getHost().getHostName())
              .withEntityNewValue(applicationHost)
              .withShortDescription("Host " + applicationHost.getHost().getHostName() + " deleted")
              .withTitle("Host " + applicationHost.getHost().getHostName() + " deleted")
              .build());
      return delete;
    }
    return false;
  }

  @Override
  public void deleteByInfra(String infraId) {
    wingsPersistence.createQuery(ApplicationHost.class).field("infraId").equal(infraId).asList().forEach(this ::delete);
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
    Tag configTag = validateAndFetchTag(baseHost.getAppId(), envId, baseHost.getConfigTag());

    List<String> duplicateHostNames = new ArrayList<>();
    List<ApplicationHost> savedAppHosts = new ArrayList<>();

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
      host.setConfigTag(configTag);
      try {
        Host savedHost = save(host);
        ApplicationHost savedAppHost = saveApplicationHost(baseHost.getAppId(), envId, savedHost);
        savedAppHosts.add(savedAppHost);
      } catch (DuplicateKeyException dupEx) {
        logger.error("Duplicate host insertion for host {} {}", host, dupEx.getMessage());
        duplicateHostNames.add(host.getHostName());
      }
    });
    //    serviceTemplates.forEach(serviceTemplate -> serviceTemplateService.addHosts(serviceTemplate, savedAppHosts));

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

  private ApplicationHost saveApplicationHost(String appId, String envId, Host host) {
    return wingsPersistence.saveAndGet(ApplicationHost.class,
        ApplicationHost.Builder.anApplicationHost()
            .withAppId(appId)
            .withEnvId(envId)
            .withConfigTag(host.getConfigTag())
            .withHost(host)
            .build());
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
    } else {
      fetchedTag = tagService.getDefaultTagForUntaggedHosts(appId, envId);
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
