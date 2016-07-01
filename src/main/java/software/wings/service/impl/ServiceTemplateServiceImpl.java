package software.wings.service.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;

import com.google.common.collect.ImmutableMap;

import org.eclipse.jetty.util.ArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
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
import software.wings.service.intfc.TagService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/4/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceTemplateServiceImpl implements ServiceTemplateService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TagService tagService;
  @Inject private ConfigService configService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private HostService hostService;
  @Inject private InfraService infraService;
  @Inject private ExecutorService executorService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<ServiceTemplate> list(PageRequest<ServiceTemplate> pageRequest) {
    return wingsPersistence.query(ServiceTemplate.class, pageRequest);
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
   * @see software.wings.service.intfc.ServiceTemplateService#updateHosts(java.lang.String, java.lang.String,
   * java.util.List)
   */
  @Override
  public ServiceTemplate updateHosts(String appId, String envId, String serviceTemplateId, List<String> hostIds) {
    ServiceTemplate serviceTemplate = get(appId, envId, serviceTemplateId);
    String infraId = infraService.getInfraIdByEnvId(appId, envId);
    List<Host> hosts = hostIds.stream()
                           .map(hostId -> hostService.get(appId, infraId, hostId))
                           .filter(Objects::nonNull)
                           .collect(toList());

    List<Host> alreadyMappedHosts = serviceTemplate.getHosts();
    updateServiceInstances(serviceTemplate, hosts, alreadyMappedHosts);
    wingsPersistence.updateFields(ServiceTemplate.class, serviceTemplateId, ImmutableMap.of("hosts", hosts));
    return get(appId, envId, serviceTemplateId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#updateTags(java.lang.String, java.lang.String,
   * java.util.List)
   */
  @Override
  public ServiceTemplate updateTags(String appId, String envId, String serviceTemplateId, List<String> tagIds) {
    List<Tag> newTags = tagIds.stream().map(tagId -> tagService.get(appId, envId, tagId)).collect(toList());
    Set<Tag> newLeafTags =
        newTags.stream().map(tag -> tagService.getLeafTags(tag)).flatMap(List::stream).collect(toSet());

    List<Host> newHostsToBeMapped = hostService.getHostsByTags(appId, envId, newLeafTags.stream().collect(toList()));

    ServiceTemplate serviceTemplate = get(appId, envId, serviceTemplateId);
    List<Host> existingMappedHosts =
        hostService.getHostsByTags(appId, envId, serviceTemplate.getLeafTags().stream().collect(toList()));

    updateServiceInstances(serviceTemplate, newHostsToBeMapped, existingMappedHosts);
    wingsPersistence.updateFields(
        ServiceTemplate.class, serviceTemplateId, ImmutableMap.of("tags", newTags, "leafTags", newLeafTags));

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
    return wingsPersistence.query(Host.class, pageRequest); // FIXME
  }

  @Override
  public void deleteHostFromTemplates(Host host) {
    deleteDirectlyMappedHosts(host);
    deleteHostsMappedByTags(host);
  }

  private void deleteHostsMappedByTags(Host host) {
    host.getTags()
        .stream()
        .map(this ::getTemplatesByLeafTag)
        .flatMap(List::stream)
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
    return tags.stream()
        .map(this ::getTemplatesByMappedTags)
        .flatMap(List::stream)
        .collect(Collectors.toSet())
        .stream()
        .collect(Collectors.toList());
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
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceTemplate.class)
                                .field("appId")
                                .equal(appId)
                                .field("envId")
                                .equal(envId)
                                .field(ID_KEY)
                                .equal(serviceTemplateId));
    executorService.submit(() -> serviceInstanceService.deleteByServiceTemplate(appId, envId, serviceTemplateId));
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
        wingsPersistence.deleteFromList(ServiceTemplate.class, serviceTemplate.getUuid(), "hosts", host.getUuid());
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
    /* override order(left to right): Service -> Env -> [Tag Hierarchy] -> Host */

    List<ConfigFile> serviceConfigFiles =
        configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, serviceTemplate.getService().getUuid());
    List<ConfigFile> envConfigFiles = configService.getConfigFilesForEntity(templateId, serviceTemplate.getEnvId());

    // service -> env overrides
    logger.info("Apply env config.");
    List<ConfigFile> envComputedConfigFiles = overrideConfigFiles(serviceConfigFiles, envConfigFiles);

    // flatten tag hierarchy and [tag -> tag] overrides
    logger.info("Flatten Tag hierarchy and apply config overrides");
    List<Tag> leafTagNodes = applyOverrideAndGetLeafTags(serviceTemplate);

    // env->tag override
    logger.info("Apply tag override on tags");
    for (Tag tag : leafTagNodes) {
      tag.setConfigFiles(overrideConfigFiles(envComputedConfigFiles, tag.getConfigFiles()));
    }

    // Host override
    logger.info("Apply host overrides");
    Map<String, List<ConfigFile>> computedHostConfigs = new HashMap<>();

    // Untagged hosts override: env->host
    logger.info("Apply host overrides for untagged hosts");
    for (Host host : serviceTemplate.getHosts()) {
      List<ConfigFile> configFiles = configService.getConfigFilesForEntity(templateId, host.getUuid());
      computedHostConfigs.put(host.getUuid(), overrideConfigFiles(envConfigFiles, configFiles));
    }

    // Tagged hosts
    logger.info("Apply host overrides for tagged hosts");
    for (Tag tag : leafTagNodes) {
      List<Host> taggedHosts = wingsPersistence.createQuery(Host.class).field("tags").equal(tag.getUuid()).asList();
      for (Host host : taggedHosts) {
        computedHostConfigs.put(host.getUuid(),
            overrideConfigFiles(
                tag.getConfigFiles(), configService.getConfigFilesForEntity(templateId, host.getUuid())));
      }
    }
    return computedHostConfigs;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceTemplateService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public ServiceTemplate get(String appId, String envId, String serviceTemplateId) {
    return wingsPersistence.get(ServiceTemplate.class, appId, serviceTemplateId);
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

  private List<Tag> applyOverrideAndGetLeafTags(ServiceTemplate serviceTemplate) {
    List<Tag> leafTagNodes = new ArrayList<>();
    Tag rootTag = tagService.getRootConfigTag(serviceTemplate.getAppId(), serviceTemplate.getEnvId());
    if (rootTag == null) {
      return leafTagNodes;
    }
    rootTag.getConfigFiles().addAll(
        configService.getConfigFilesForEntity(serviceTemplate.getUuid(), rootTag.getUuid()));

    Queue<Tag> queue = new ArrayQueue<>();
    queue.add(rootTag);

    while (!queue.isEmpty()) {
      Tag root = queue.poll();
      leafTagNodes.add(root);
      for (Tag child : root.getChildren()) {
        child.getConfigFiles().addAll(
            configService.getConfigFilesForEntity(serviceTemplate.getUuid(), child.getUuid()));
        child.setConfigFiles(overrideConfigFiles(root.getConfigFiles(), child.getConfigFiles()));
        queue.add(child);
      }
    }
    return leafTagNodes;
  }
}
