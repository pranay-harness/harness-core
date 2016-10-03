package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityType.HOST;
import static software.wings.beans.EntityType.TAG;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.eclipse.jetty.util.ArrayQueue;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.TagService;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 4/4/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceTemplateServiceImpl implements ServiceTemplateService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TagService tagService;
  @Inject private ConfigService configService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private HostService hostService;
  @Inject private InfraService infraService;
  @Inject private ExecutorService executorService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<ServiceTemplate> list(PageRequest<ServiceTemplate> pageRequest, boolean withDetails) {
    PageResponse<ServiceTemplate> pageResponse = wingsPersistence.query(ServiceTemplate.class, pageRequest);
    if (withDetails) {
      pageResponse.getResponse().forEach(template -> {
        if (template.getTags().size() != 0) {
          template.setTaggedHosts(hostService.getHostsByTags(
              template.getAppId(), template.getEnvId(), new ArrayList<>(template.getLeafTags())));
        }
        template.setConfigFiles(getOverrideFiles(template));
      });
    }
    return pageResponse;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#save(software.wings.beans.ServiceTemplate)
   */
  @Override
  public ServiceTemplate save(ServiceTemplate serviceTemplate) {
    return wingsPersistence.saveAndGet(ServiceTemplate.class, serviceTemplate);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#update(software.wings.beans.ServiceTemplate)
   */
  @Override
  public ServiceTemplate update(ServiceTemplate serviceTemplate) {
    wingsPersistence.updateFields(ServiceTemplate.class, serviceTemplate.getUuid(),
        ImmutableMap.of("name", serviceTemplate.getName(), "description", serviceTemplate.getDescription(), "service",
            serviceTemplate.getService()));
    return wingsPersistence.get(ServiceTemplate.class, serviceTemplate.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public ServiceTemplate get(String appId, String envId, String serviceTemplateId, boolean withDetails) {
    ServiceTemplate serviceTemplate = get(appId, serviceTemplateId);
    if (withDetails) {
      if (serviceTemplate.getTags().size() != 0) {
        serviceTemplate.setTaggedHosts(
            hostService.getHostsByTags(appId, envId, new ArrayList<>(serviceTemplate.getLeafTags())));
      }
      serviceTemplate.setConfigFiles(getOverrideFiles(serviceTemplate));
    }
    return serviceTemplate;
  }

  @Override
  public ServiceTemplate get(String appId, String serviceTemplateId) {
    ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, appId, serviceTemplateId);
    Validator.notNullCheck("ServiceTemplate", serviceTemplate);
    return serviceTemplate;
  }

  @Override
  public ServiceTemplate addHosts(ServiceTemplate template, List<Host> hosts) {
    List<Host> allHosts =
        Stream.concat(hosts.stream(), template.getHosts().stream()).distinct().collect(Collectors.toList());
    return updateTemplateAndServiceInstance(template, allHosts);
  }

  @Override
  public List<Key<ServiceTemplate>> getTemplateRefKeysByService(String appId, String serviceId, String envId) {
    Query<ServiceTemplate> templateQuery = wingsPersistence.createQuery(ServiceTemplate.class)
                                               .field("appId")
                                               .equal(appId)
                                               .field("service")
                                               .equal(serviceId);
    if (!isNullOrEmpty(envId)) {
      templateQuery.field("envId").equal(envId);
    }
    return templateQuery.asKeyList();
  }

  @Override
  public List<ConfigFile> getOverrideFiles(String appId, String envId, String templateId) {
    ServiceTemplate serviceTemplate = get(appId, envId, templateId, false);
    return getOverrideFiles(serviceTemplate);
  }

  @Override
  public void updateDefaultServiceTemplateName(
      String appId, String serviceId, String oldServiceName, String newServiceName) {
    Query<ServiceTemplate> query = wingsPersistence.createQuery(ServiceTemplate.class)
                                       .field("appId")
                                       .equal(appId)
                                       .field("service")
                                       .equal(serviceId)
                                       .field("defaultServiceTemplate")
                                       .equal(true)
                                       .field("name")
                                       .equal(oldServiceName);
    UpdateOperations<ServiceTemplate> updateOperations =
        wingsPersistence.createUpdateOperations(ServiceTemplate.class).set("name", newServiceName);
    wingsPersistence.update(query, updateOperations);
  }

  private List<ConfigFile> getOverrideFiles(ServiceTemplate template) {
    List<ConfigFile> serviceConfigFiles = configService.getConfigFilesForEntity(
        template.getAppId(), DEFAULT_TEMPLATE_ID, template.getService().getUuid());
    List<ConfigFile> overrideConfigFiles =
        configService.getConfigFileByTemplate(template.getAppId(), template.getEnvId(), template);

    ImmutableMap<String, ConfigFile> serviceConfigFilesMap =
        Maps.uniqueIndex(serviceConfigFiles, ConfigFile::getRelativeFilePath);
    Map<String, List<ConfigFile>> overridePathMap =
        overrideConfigFiles.stream().collect(groupingBy(ConfigFile::getOverridePath));

    overrideConfigFiles.stream()
        .filter(configFile -> asList(TAG, HOST).contains(configFile.getEntityType()))
        .forEach(configFile -> {
          String path = configFile.getOverridePath();
          while (!isNullOrEmpty(path)) {
            String parentPath = substringBeforeLast(path, "/");

            if (overridePathMap.containsKey(parentPath) && !parentPath.equals(path)) {
              Optional<ConfigFile> parentFile =
                  overridePathMap.get(parentPath)
                      .stream()
                      .filter(parentConfigFile
                          -> parentConfigFile.getRelativeFilePath().equals(configFile.getRelativeFilePath()))
                      .findFirst();
              if (parentFile.isPresent()) {
                configFile.setOverriddenConfigFile(parentFile.get());
                break;
              }
            } else if (path.equals(
                           parentPath)) { // no more parent path possible. override must be on service config file
              configFile.setOverriddenConfigFile(serviceConfigFilesMap.get(configFile.getRelativeFilePath()));
              break;
            }
            path = parentPath;
          }
        });

    return overrideConfigFiles;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#updateHosts(java.lang.String, java.lang.String,
   * java.util.List)
   */
  @Override
  public ServiceTemplate updateHosts(String appId, String envId, String serviceTemplateId, List<String> hostIds) {
    Infra infra = infraService.getInfraByEnvId(appId, envId);
    List<Host> hosts = hostIds.stream()
                           .map(hostId -> hostService.get(appId, infra.getUuid(), hostId))
                           .filter(Objects::nonNull)
                           .collect(toList());

    ServiceTemplate serviceTemplate = get(appId, envId, serviceTemplateId, true);

    if (serviceTemplate.getMappedBy().equals(TAG)) {
      serviceTemplate = updateTags(appId, envId, serviceTemplateId, asList()); // remove existing tags
      serviceTemplate.setMappedBy(EntityType.HOST);
    }
    return updateTemplateAndServiceInstance(serviceTemplate, hosts);
  }

  private ServiceTemplate updateTemplateAndServiceInstance(ServiceTemplate serviceTemplate, List<Host> hosts) {
    List<Host> alreadyMappedHosts = serviceTemplate.getHosts();
    updateServiceInstances(serviceTemplate, hosts, alreadyMappedHosts);
    wingsPersistence.updateFields(ServiceTemplate.class, serviceTemplate.getUuid(),
        ImmutableMap.of("mappedBy", serviceTemplate.getMappedBy(), "hosts", hosts));
    return get(serviceTemplate.getAppId(), serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#updateTags(java.lang.String, java.lang.String,
   * java.util.List)
   */
  @Override
  public ServiceTemplate updateTags(String appId, String envId, String serviceTemplateId, List<String> tagIds) {
    List<Tag> newTags = tagIds.stream().map(tagId -> tagService.get(appId, envId, tagId)).collect(toList());
    Set<Tag> newLeafTags =
        newTags.stream().map(tag -> tagService.getLeafTagInSubTree(tag)).flatMap(List::stream).collect(toSet());

    List<Host> newHostsToBeMapped = hostService.getHostsByTags(appId, envId, newLeafTags.stream().collect(toList()));

    ServiceTemplate serviceTemplate = get(appId, envId, serviceTemplateId, true);
    if (serviceTemplate.getMappedBy().equals(HOST)) {
      serviceTemplate = updateHosts(appId, envId, serviceTemplateId, emptyList()); // remove existing host mapping
      serviceTemplate.setMappedBy(TAG);
    }

    List<Host> existingMappedHosts =
        hostService.getHostsByTags(appId, envId, serviceTemplate.getLeafTags().stream().collect(toList()));

    updateServiceInstances(serviceTemplate, newHostsToBeMapped, existingMappedHosts);
    wingsPersistence.updateFields(ServiceTemplate.class, serviceTemplateId,
        ImmutableMap.of("mappedBy", serviceTemplate.getMappedBy(), "tags", newTags, "leafTags", newLeafTags));

    return wingsPersistence.get(ServiceTemplate.class, serviceTemplateId);
  }

  private void updateServiceInstances(ServiceTemplate serviceTemplate, List<Host> newHosts, List<Host> existingHosts) {
    List<Host> addHostsList = new ArrayList<>();
    List<Host> deleteHostList = new ArrayList<>();

    newHosts.forEach(host -> {
      if (!existingHosts.contains(host)) {
        addHostsList.add(host);
      }
    });

    existingHosts.forEach(host -> {
      if (!newHosts.contains(host)) {
        deleteHostList.add(host);
      }
    });
    serviceInstanceService.updateInstanceMappings(serviceTemplate, addHostsList, deleteHostList);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#getTaggedHosts(java.lang.String,
   * software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Host> getTaggedHosts(
      String appId, String envId, String templateId, PageRequest<Host> pageRequest) {
    ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, appId, templateId);
    List<Tag> tags = serviceTemplate.getTags();
    pageRequest.addFilter("tags", tags, IN);
    return hostService.list(pageRequest);
  }

  @Override
  public void deleteHostFromTemplates(Host host) {
    deleteDirectlyMappedHosts(host);
    deleteHostsMappedByTags(host);
  }

  private void deleteHostsMappedByTags(Host host) {
    getTemplatesByLeafTag(host.getConfigTag())
        .forEach(
            serviceTemplate -> serviceInstanceService.updateInstanceMappings(serviceTemplate, asList(), asList(host)));
  }

  private void deleteDirectlyMappedHosts(Host host) {
    List<ServiceTemplate> serviceTemplates = wingsPersistence.createQuery(ServiceTemplate.class)
                                                 .field("appId")
                                                 .equal(host.getAppId())
                                                 .field("hosts")
                                                 .equal(host.getUuid())
                                                 .asList();
    deleteHostFromATemplate(host, serviceTemplates);
  }

  @Override
  public List<ServiceTemplate> getTemplatesByLeafTag(Tag tag) {
    return wingsPersistence.createQuery(ServiceTemplate.class)
        .field("appId")
        .equal(tag.getAppId())
        .field("envId")
        .equal(tag.getEnvId())
        .field("leafTags")
        .equal(tag.getUuid())
        .asList();
  }

  @Override
  public List<ServiceTemplate> getTemplateByMappedTags(List<Tag> tags) {
    if (tags.size() == 0) {
      return new ArrayList<>();
    }
    return tags.stream().map(this ::getTemplatesByMappedTags).flatMap(List::stream).distinct().collect(toList());
  }

  @Override
  public void addLeafTag(ServiceTemplate template, Tag tag) {
    wingsPersistence.update(
        template, wingsPersistence.createUpdateOperations(ServiceTemplate.class).add("leafTags", tag));
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#delete(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public void delete(String appId, String envId, String serviceTemplateId) {
    boolean deleted = wingsPersistence.delete(wingsPersistence.createQuery(ServiceTemplate.class)
                                                  .field("appId")
                                                  .equal(appId)
                                                  .field("envId")
                                                  .equal(envId)
                                                  .field(ID_KEY)
                                                  .equal(serviceTemplateId));
    if (deleted) {
      executorService.submit(() -> serviceInstanceService.deleteByServiceTemplate(appId, envId, serviceTemplateId));
      configService.deleteByTemplateId(appId, serviceTemplateId);
    }
  }

  @Override
  public void deleteByEnv(String appId, String envId) {
    wingsPersistence.createQuery(ServiceTemplate.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .asList()
        .forEach(serviceTemplate -> delete(appId, envId, serviceTemplate.getUuid()));
  }

  @Override
  public void deleteByService(String appId, String serviceId) {
    wingsPersistence.createQuery(ServiceTemplate.class)
        .field("appId")
        .equal(appId)
        .field("service")
        .equal(serviceId)
        .asList()
        .forEach(serviceTemplate
            -> delete(serviceTemplate.getAppId(), serviceTemplate.getEnvId(), serviceTemplate.getUuid()));
  }

  @Override
  public void createDefaultTemplatesByEnv(Environment env) {
    List<Service> services = serviceResourceService.findServicesByApp(env.getAppId());
    services.forEach(service
        -> save(aServiceTemplate()
                    .withAppId(service.getAppId())
                    .withEnvId(env.getUuid())
                    .withService(service)
                    .withName(service.getName())
                    .build()));
  }

  @Override
  public void createDefaultTemplatesByService(Service service) {
    List<Environment> environments = environmentService.getEnvByApp(service.getAppId());
    environments.forEach(environment
        -> save(aServiceTemplate()
                    .withAppId(service.getAppId())
                    .withEnvId(environment.getUuid())
                    .withService(service)
                    .withName(service.getName())
                    .withDefaultServiceTemplate(true)
                    .build()));
  }

  private List<ServiceTemplate> getTemplatesByMappedTags(Tag tag) {
    return wingsPersistence.createQuery(ServiceTemplate.class)
        .field("appId")
        .equal(tag.getAppId())
        .field("envId")
        .equal(tag.getEnvId())
        .field("tags")
        .equal(tag.getUuid())
        .asList();
  }

  private void deleteHostFromATemplate(Host host, List<ServiceTemplate> serviceTemplates) {
    if (serviceTemplates != null) {
      serviceTemplates.forEach(serviceTemplate -> {
        wingsPersistence.deleteFromList(
            ServiceTemplate.class, host.getAppId(), serviceTemplate.getUuid(), "hosts", host.getUuid());
        serviceInstanceService.updateInstanceMappings(serviceTemplate, asList(), asList(host));
      });
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#computedConfigFiles(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public Map<String, List<ConfigFile>> computedConfigFiles(String appId, String envId, String templateId) {
    ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, templateId);
    if (serviceTemplate == null) {
      return new HashMap<>();
    }
    /* override order(left to right): Service -> [Tag Hierarchy] -> Host */

    List<ConfigFile> serviceConfigFiles =
        configService.getConfigFilesForEntity(appId, DEFAULT_TEMPLATE_ID, serviceTemplate.getService().getUuid());

    // flatten tag hierarchy and [tag -> tag] overrides
    logger.info("Flatten Tag hierarchy and getInfo config overrides");
    List<Tag> leafTagNodes = applyOverrideAndGetLeafTags(serviceTemplate);

    // service->tag override
    logger.info("Apply tag override on tags");
    for (Tag tag : leafTagNodes) {
      tag.setConfigFiles(overrideConfigFiles(serviceConfigFiles, tag.getConfigFiles()));
    }

    // Tag -> Host override
    logger.info("Apply host overrides");
    Map<String, List<ConfigFile>> computedHostConfigs = new HashMap<>();

    logger.info("Apply host overrides for tagged hosts");
    for (Tag tag : leafTagNodes) {
      List<Host> taggedHosts = hostService.getHostsByTags(tag.getAppId(), tag.getEnvId(), asList(tag));
      for (Host host : taggedHosts) {
        computedHostConfigs.put(host.getUuid(),
            overrideConfigFiles(
                tag.getConfigFiles(), configService.getConfigFilesForEntity(appId, templateId, host.getUuid())));
      }
    }
    return computedHostConfigs;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#computedConfigFiles(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public Map<String, List<ServiceVariable>> computeServiceVariables(String appId, String envId, String templateId) {
    ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, templateId);
    if (serviceTemplate == null) {
      return new HashMap<>();
    }
    /* override order(left to right): Service -> [Tag Hierarchy] -> Host */

    List<ServiceVariable> serviceConfigFiles = serviceVariableService.getServiceVariablesForEntity(
        appId, DEFAULT_TEMPLATE_ID, serviceTemplate.getService().getUuid());

    // flatten tag hierarchy and [tag -> tag] overrides
    logger.info("Flatten Tag hierarchy and getInfo config overrides");
    List<Tag> leafTagNodes = applyOverrideAndGetLeafTags(serviceTemplate);

    // service->tag override
    logger.info("Apply tag override on tags");
    for (Tag tag : leafTagNodes) {
      tag.setServiceVariables(overrideServiceSettings(serviceConfigFiles, tag.getServiceVariables()));
    }

    // Tag -> Host override
    logger.info("Apply host overrides");
    Map<String, List<ServiceVariable>> computedHostConfigs = new HashMap<>();

    logger.info("Apply host overrides for tagged hosts");
    for (Tag tag : leafTagNodes) {
      List<Host> taggedHosts = hostService.getHostsByTags(tag.getAppId(), tag.getEnvId(), asList(tag));
      for (Host host : taggedHosts) {
        computedHostConfigs.put(host.getUuid(),
            overrideServiceSettings(tag.getServiceVariables(),
                serviceVariableService.getServiceVariablesForEntity(appId, templateId, host.getUuid())));
      }
    }
    return computedHostConfigs;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#overrideConfigFiles(java.util.List, java.util.List)
   */
  @Override
  public List<ConfigFile> overrideConfigFiles(List<ConfigFile> existingFiles, List<ConfigFile> newFiles) {
    logger.info("Config files before overrides [{}]", existingFiles.toString());
    logger.info("New override config files [{}]", newFiles != null ? newFiles.toString() : null);
    if (newFiles != null && !newFiles.isEmpty()) {
      existingFiles = Stream.concat(newFiles.stream(), existingFiles.stream())
                          .filter(new TreeSet<>(Comparator.comparing(ConfigFile::getName))::add)
                          .collect(toList());
    }
    logger.info("Config files after overrides [{}]", existingFiles.toString());
    return existingFiles;
  }

  /**
   * Override service settings list.
   *
   * @param existingFiles the existing files
   * @param newFiles      the new files
   * @return the list
   */
  public List<ServiceVariable> overrideServiceSettings(
      List<ServiceVariable> existingFiles, List<ServiceVariable> newFiles) {
    logger.info("Config files before overrides [{}]", existingFiles.toString());
    logger.info("New override config files [{}]", newFiles != null ? newFiles.toString() : null);
    if (newFiles != null && !newFiles.isEmpty()) {
      existingFiles = Stream.concat(newFiles.stream(), existingFiles.stream())
                          .filter(new TreeSet<>(Comparator.comparing(ServiceVariable::getName))::add)
                          .collect(toList());
    }
    logger.info("Config files after overrides [{}]", existingFiles.toString());
    return existingFiles;
  }

  private List<Tag> applyOverrideAndGetLeafTags(ServiceTemplate serviceTemplate) {
    List<Tag> leafTagNodes = new ArrayList<>();
    Tag rootTag = tagService.getRootConfigTag(serviceTemplate.getAppId(), serviceTemplate.getEnvId());
    if (rootTag == null) {
      return leafTagNodes;
    }
    rootTag.getConfigFiles().addAll(configService.getConfigFilesForEntity(
        serviceTemplate.getAppId(), serviceTemplate.getUuid(), rootTag.getUuid()));

    Queue<Tag> queue = new ArrayQueue<>();
    queue.add(rootTag);

    while (!queue.isEmpty()) {
      Tag parentTag = queue.poll();
      if (parentTag.getChildren().size() == 0) {
        leafTagNodes.add(parentTag);
      } else {
        for (Tag child : parentTag.getChildren()) {
          child.getConfigFiles().addAll(configService.getConfigFilesForEntity(
              serviceTemplate.getAppId(), serviceTemplate.getUuid(), child.getUuid()));
          child.setConfigFiles(overrideConfigFiles(parentTag.getConfigFiles(), child.getConfigFiles()));
          queue.add(child);
        }
      }
    }
    return leafTagNodes;
  }

  private List<Tag> applyOverrideAndGetLeafTagsWithServiceSettings(ServiceTemplate serviceTemplate) {
    List<Tag> leafTagNodes = new ArrayList<>();
    Tag rootTag = tagService.getRootConfigTag(serviceTemplate.getAppId(), serviceTemplate.getEnvId());
    if (rootTag == null) {
      return leafTagNodes;
    }
    rootTag.getServiceVariables().addAll(serviceVariableService.getServiceVariablesForEntity(
        serviceTemplate.getAppId(), serviceTemplate.getUuid(), rootTag.getUuid()));

    Queue<Tag> queue = new ArrayQueue<>();
    queue.add(rootTag);

    while (!queue.isEmpty()) {
      Tag parentTag = queue.poll();
      if (parentTag.getChildren().size() == 0) {
        leafTagNodes.add(parentTag);
      } else {
        for (Tag child : parentTag.getChildren()) {
          child.getServiceVariables().addAll(serviceVariableService.getServiceVariablesForEntity(
              serviceTemplate.getAppId(), serviceTemplate.getUuid(), child.getUuid()));
          child.setServiceVariables(
              overrideServiceSettings(parentTag.getServiceVariables(), child.getServiceVariables()));
          queue.add(child);
        }
      }
    }
    return leafTagNodes;
  }
}
