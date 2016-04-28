package software.wings.service.impl;

import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.eclipse.jetty.util.ArrayQueue;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 4/4/16.
 */
@Singleton
public class ServiceTemplateServiceImpl implements ServiceTemplateService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TagService tagService;

  @Override
  public PageResponse<ServiceTemplate> list(String envId, PageRequest<ServiceTemplate> pageRequest) {
    return wingsPersistence.query(ServiceTemplate.class, pageRequest);
  }

  @Override
  public ServiceTemplate createServiceTemplate(ServiceTemplate serviceTemplate) {
    return wingsPersistence.saveAndGet(ServiceTemplate.class, serviceTemplate);
  }

  @Override
  public ServiceTemplate updateServiceTemplate(ServiceTemplate serviceTemplate) {
    wingsPersistence.updateFields(ServiceTemplate.class, serviceTemplate.getUuid(),
        ImmutableMap.of("name", serviceTemplate.getName(), "description", serviceTemplate.getDescription()));
    return wingsPersistence.get(ServiceTemplate.class, serviceTemplate.getUuid());
  }

  @Override
  public ServiceTemplate updateHostAndTags(String serviceTemplateId, List<String> tagIds, List<String> hostIds) {
    List<Tag> tags = new ArrayList<>();
    for (String tagId : tagIds) {
      tags.add(wingsPersistence.get(Tag.class, tagId));
    }
    List<Host> hosts = new ArrayList<>();
    for (String hostId : hostIds) {
      hosts.add(wingsPersistence.get(Host.class, hostId));
    }
    wingsPersistence.updateFields(
        ServiceTemplate.class, serviceTemplateId, ImmutableMap.of("hosts", hosts, "tags", tags));
    return wingsPersistence.get(ServiceTemplate.class, serviceTemplateId);
  }

  @Override
  public Map<String, List<ConfigFile>> computedConfigFiles(String templateId) {
    ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, templateId);
    Service service = wingsPersistence.get(Service.class, serviceTemplate.getServiceId());
    Environment environment = wingsPersistence.get(Environment.class, serviceTemplate.getEnvId());

    /* override order(left to right): Service -> Env -> [Tag Hierarchy] -> Host */

    List<ConfigFile> serviceConfigFiles = getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, service.getUuid());
    List<ConfigFile> envConfigFiles = getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, environment.getUuid());

    // Env overrides
    List<ConfigFile> envComputedConfigFiles = computeOverride(serviceConfigFiles, envConfigFiles);

    // Tag overrides
    List<Tag> leafTagNodes = applyOverrideAndGetLeafTags(serviceTemplate);

    // Host override

    Map<String, List<ConfigFile>> computedHostConfigs = new HashMap<>();

    // Untagged hosts override: env->host
    for (Host host : serviceTemplate.getHosts()) {
      List<ConfigFile> configFiles = getConfigFilesForEntity(templateId, host.getUuid());
      computedHostConfigs.put(host.getUuid(), computeOverride(envConfigFiles, configFiles));
    }

    // Tagged hosts
    for (Tag tag : leafTagNodes) {
      List<Host> taggedHosts = wingsPersistence.createQuery(Host.class).field("tags").hasThisElement(tag).asList();
      for (Host host : taggedHosts) {
        computedHostConfigs.put(host.getUuid(), computeOverride(tag.getConfigFiles(), host.getConfigFiles()));
      }
    }
    return computedHostConfigs;
  }

  private List<Tag> applyOverrideAndGetLeafTags(ServiceTemplate serviceTemplate) {
    List<Tag> leafTagNodes = new ArrayList<>();
    List<Tag> rootTags = tagService.getRootConfigTags(serviceTemplate.getEnvId());

    Queue<Tag> queue = new ArrayQueue<>();
    queue.addAll(rootTags);

    while (!queue.isEmpty()) {
      Tag root = queue.poll();
      if (root.getLinkedTags() == null || root.getLinkedTags().isEmpty()) {
        leafTagNodes.add(root);
      } else {
        for (Tag child : root.getLinkedTags()) {
          child.setConfigFiles(computeOverride(root.getConfigFiles(), child.getConfigFiles()));
        }
      }
    }
    return leafTagNodes;
  }

  private List<ConfigFile> computeOverride(List<ConfigFile> existingFiles, List<ConfigFile> newFiles) {
    if (newFiles != null && !newFiles.isEmpty()) {
      existingFiles = Stream.concat(existingFiles.stream(), newFiles.stream())
                          .filter(new TreeSet<>(Comparator.comparing(ConfigFile::getName))::add)
                          .collect(Collectors.toList());
    }
    return existingFiles;
  }

  private List<ConfigFile> getConfigFilesForEntity(String templateId, String uuid) {
    return wingsPersistence.createQuery(ConfigFile.class)
        .field("templateId")
        .equal(templateId)
        .field("entityId")
        .equal(uuid)
        .asList();
  }
}
