package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ApplicationHost.Builder.anApplicationHost;
import static software.wings.beans.History.Builder.aHistory;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.ResponseMessage.Builder.aResponseMessage;
import static software.wings.common.NotificationMessageResolver.ADD_HOST_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.HOST_DELETE_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.commons.collections.IteratorUtils;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
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
import software.wings.beans.infrastructure.ApplicationHostUsage;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
  @Inject private AppService appService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<ApplicationHost> list(PageRequest<ApplicationHost> req) {
    return wingsPersistence.query(ApplicationHost.class, req);
  }

  @Override
  public ApplicationHost get(String appId, String envId, String hostId) {
    ApplicationHost applicationHost = wingsPersistence.createQuery(ApplicationHost.class)
                                          .field(ID_KEY)
                                          .equal(hostId)
                                          .field("envId")
                                          .equal(envId)
                                          .field("appId")
                                          .equal(appId)
                                          .get();
    notNullCheck("ApplicationHost", applicationHost);
    return applicationHost;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#update(software.wings.beans.Host)
   */
  @Override
  public ApplicationHost update(String envId, Host host) {
    ApplicationHost applicationHost = get(host.getAppId(), envId, host.getUuid());

    if (applicationHost == null || applicationHost.getHost() == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Host doesn't exist");
    }

    ImmutableMap.Builder builder = ImmutableMap.<String, Object>builder().put("hostConnAttr", host.getHostConnAttr());
    if (host.getBastionConnAttr() != null) {
      builder.put("bastionConnAttr", host.getBastionConnAttr());
    }
    wingsPersistence.updateFields(Host.class, applicationHost.getHost().getUuid(), builder.build());

    Tag tag = validateAndFetchTag(host.getAppId(), envId, host.getConfigTag());

    if (tag != null && tag.equals(applicationHost.getConfigTag())) {
      return get(applicationHost.getAppId(), applicationHost.getEnvId(), applicationHost.getUuid());
    }

    // Tag update -> should update host mapped in template

    if (tag == null) {
      tag = tagService.getDefaultTagForUntaggedHosts(applicationHost.getAppId(), envId);
    }

    List<ApplicationHost> hostsByTags = getHostsByTags(host.getAppId(), envId, asList(tag));
    hostsByTags.add(applicationHost);
    tagService.tagHosts(tag, hostsByTags);

    List<ServiceTemplate> serviceTemplates =
        validateAndFetchServiceTemplate(host.getAppId(), host.getServiceTemplates());
    ApplicationHost appHost = get(applicationHost.getAppId(), applicationHost.getEnvId(), host.getUuid());
    serviceTemplates.forEach(serviceTemplate -> serviceTemplateService.addHosts(serviceTemplate, asList(appHost)));
    return appHost;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#bulkSave(software.wings.beans.Host, java.util.List)
   */
  @Override
  public ResponseMessage bulkSave(String infraId, String envId, Host baseHost) {
    Set<String> hostNames = baseHost.getHostNames()
                                .stream()
                                .filter(hostName -> !isNullOrEmpty(hostName))
                                .map(String::trim)
                                .collect(Collectors.toSet());
    List<ServiceTemplate> serviceTemplates =
        validateAndFetchServiceTemplate(baseHost.getAppId(), baseHost.getServiceTemplates());
    Infrastructure infrastructure = infraService.get(infraId);
    Tag configTag = validateAndFetchTag(baseHost.getAppId(), envId, baseHost.getConfigTag());

    List<ApplicationHost> applicationHosts =
        saveApplicationHosts(envId, baseHost, hostNames, infrastructure, configTag);

    serviceTemplates.forEach(serviceTemplate -> serviceTemplateService.addHosts(serviceTemplate, applicationHosts));

    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(baseHost.getAppId())
            .withDisplayText(getDecoratedNotificationMessage(ADD_HOST_NOTIFICATION,
                ImmutableMap.of("COUNT", Integer.toString(hostNames.size()), "ENV_NAME",
                    environmentService.get(baseHost.getAppId(), envId, false).getName())))
            .build());
    // TODO: history entry for bulk save

    return aResponseMessage().build();
  }

  private Host getOrCreateInfraHost(Infrastructure infrastructure, String hostName, Host baseHost) {
    Host host = wingsPersistence.createQuery(Host.class)
                    .field("hostName")
                    .equal(hostName)
                    .field("infraId")
                    .equal(infrastructure.getUuid())
                    .get();
    if (host == null) {
      host = aHost()
                 .withHostName(hostName)
                 .withAppId(infrastructure.getAppId())
                 .withInfraId(infrastructure.getUuid())
                 .withHostConnAttr(baseHost.getHostConnAttr())
                 .build();
      SettingAttribute bastionConnAttr =
          validateAndFetchBastionHostConnectionReference(baseHost.getAppId(), baseHost.getBastionConnAttr());
      if (bastionConnAttr != null) {
        host.setBastionConnAttr(bastionConnAttr);
      }
      host = wingsPersistence.saveAndGet(Host.class, host);
      infraService.applyApplicableMappingRules(infrastructure, host);
    }
    return host;
  }

  public List<ApplicationHost> saveApplicationHosts(
      String envId, Host baseHost, Set<String> hostNames, Infrastructure infrastructure, Tag configTag) {
    List<ApplicationHost> applicationHosts = new ArrayList<>();

    hostNames.forEach(hostName -> {
      ApplicationHost applicationHost = wingsPersistence.createQuery(ApplicationHost.class)
                                            .field("hostName")
                                            .equal(hostName)
                                            .field("appId")
                                            .equal(baseHost.getAppId())
                                            .get();
      if (applicationHost == null) {
        Host host = getOrCreateInfraHost(infrastructure, hostName, baseHost);
        applicationHost = wingsPersistence.saveAndGet(ApplicationHost.class,
            anApplicationHost()
                .withAppId(baseHost.getAppId())
                .withEnvId(envId)
                .withConfigTag(configTag)
                .withInfraId(host.getInfraId())
                .withHostName(host.getHostName())
                .withHost(host)
                .build());
      }
      applicationHosts.add(applicationHost);
    });
    return applicationHosts;
  }

  @Override
  public int getHostCountByInfrastructure(String infraId) {
    return (int) wingsPersistence.createQuery(Host.class).field("infraId").equal(infraId).countAll();
  }

  @Override
  public List<ApplicationHostUsage> getInfrastructureHostUsageByApplication(String infraId) {
    List<Application> apps =
        appService
            .list(aPageRequest().withLimit(PageRequest.UNLIMITED).withOffset("0").addFieldsIncluded("name").build(),
                false, 0)
            .getResponse();
    ImmutableMap<String, Application> applicationsById = Maps.uniqueIndex(apps, Application::getUuid);

    Query<ApplicationHost> query = wingsPersistence.createQuery(ApplicationHost.class).field("infraId").equal(infraId);

    Iterator<ApplicationHostUsage> hostUsageIterator =
        wingsPersistence.getDatastore()
            .createAggregation(ApplicationHost.class)
            .match(query)
            .group("appId", Group.grouping("count", new Accumulator("$sum", 1)))
            .aggregate(ApplicationHostUsage.class);

    List<ApplicationHostUsage> hostUsageList = IteratorUtils.toList(hostUsageIterator);
    hostUsageList.forEach(applicationHostUsage
        -> applicationHostUsage.setAppName(applicationsById.get(applicationHostUsage.getAppId()).getName()));

    return hostUsageList;
  }

  @Override
  public void addApplicationHost(String appId, String envId, String tagId, Host host) {
    Application application = appService.get(appId);
    notNullCheck("Application", application);

    application.getEnvironments()
        .stream()
        .filter(environment -> envId == null || envId.equals(environment.getUuid()))
        .forEach(environment -> {
          Tag tag = null;
          if (tagId != null) {
            tag = tagService.get(environment.getAppId(), environment.getUuid(), tagId);
            Validator.notNullCheck("Tag", tag);
          }
          ApplicationHost applicationHost = wingsPersistence.createQuery(ApplicationHost.class)
                                                .field("hostName")
                                                .equal(host.getHostName())
                                                .field("appId")
                                                .equal(environment.getAppId())
                                                .field("envId")
                                                .equal(environment.getUuid())
                                                .get();
          if (applicationHost == null) {
            wingsPersistence.saveAndGet(ApplicationHost.class,
                anApplicationHost()
                    .withAppId(environment.getAppId())
                    .withEnvId(environment.getUuid())
                    .withConfigTag(tag)
                    .withInfraId(host.getInfraId())
                    .withHostName(host.getHostName())
                    .withHost(host)
                    .build());
          }
          // TODO: add Notification/History
        });
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#importHosts(java.lang.String, java.lang.String,
   * software.wings.utils.BoundedInputStream)
   */
  @Override
  public int importHosts(String appId, String envId, String infraId, BoundedInputStream inputStream) {
    List<Host> hosts = csvFileHelper.parseHosts(infraId, appId, envId, inputStream);
    return (int) hosts.stream().map(host -> bulkSave(infraId, envId, host)).filter(Objects::nonNull).count();
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
  public void removeTagFromHost(ApplicationHost applicationHost, Tag tag) {
    if (applicationHost.getConfigTag() != null
        && !applicationHost.getConfigTag().getTagType().equals(TagType.UNTAGGED_HOST)) {
      setTag(applicationHost, tagService.getDefaultTagForUntaggedHosts(tag.getAppId(), tag.getEnvId()));
    }
  }

  @Override
  public List<ApplicationHost> getHostsByEnv(String appId, String envId) {
    return wingsPersistence.createQuery(ApplicationHost.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .asList();
  }

  @Override
  public ApplicationHost getHostByEnv(String appId, String envId, String hostId) {
    return wingsPersistence.createQuery(ApplicationHost.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
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
  public void delete(String appId, String envId, String hostId) {
    delete(get(appId, envId, hostId));
  }

  private boolean delete(ApplicationHost applicationHost) {
    if (applicationHost != null) {
      boolean delete = wingsPersistence.delete(applicationHost);
      if (delete) {
        serviceTemplateService.deleteHostFromTemplates(applicationHost);
        executorService.submit(
            () -> configService.deleteByEntityId(applicationHost.getAppId(), applicationHost.getUuid()));
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
      return delete;
    }
    return false;
  }

  @Override
  public void deleteByInfra(String infraId) {
    wingsPersistence.createQuery(ApplicationHost.class).field("infraId").equal(infraId).asList().forEach(this ::delete);
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
    Tag fetchedTag;
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
