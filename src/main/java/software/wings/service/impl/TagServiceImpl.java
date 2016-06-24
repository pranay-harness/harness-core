package software.wings.service.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.mongodb.morphia.query.Query;
import software.wings.beans.Host;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/25/16.
 */
public class TagServiceImpl implements TagService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private HostService hostService;
  @Inject private ServiceTemplateService serviceTemplateService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#listRootTags(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Tag> listRootTags(PageRequest<Tag> request) {
    return wingsPersistence.query(Tag.class, request);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#saveTag(java.lang.String, software.wings.beans.Tag)
   */
  @Override
  public Tag saveTag(String parentTagId, Tag tag) {
    if (parentTagId == null || parentTagId.length() == 0) {
      tag.setRootTag(true);
      return wingsPersistence.saveAndGet(Tag.class, tag);
    } else {
      Tag parentTag = wingsPersistence.get(Tag.class, parentTagId);
      tag.setRootTagId(parentTag.isRootTag() ? parentTagId : parentTag.getRootTagId());
      tag = wingsPersistence.saveAndGet(Tag.class, tag);
      wingsPersistence.addToList(Tag.class, parentTagId, "children", tag.getUuid());
      updateServiceTemplateWithCommandPredecessor(tag);
      return tag;
    }
  }

  private void updateServiceTemplateWithCommandPredecessor(Tag tag) {
    List<Tag> predecessorTags = findPredecessorTags(tag);
    List<ServiceTemplate> templateByMappedTags = serviceTemplateService.getTemplateByMappedTags(predecessorTags);
    templateByMappedTags.forEach(template -> serviceTemplateService.addLeafTag(template, tag));
  }

  private List<Tag> findPredecessorTags(Tag tag) {
    List<Tag> predecessor = new ArrayList<>();
    Tag child = tag;
    Tag parent = null;
    int maxRep = 10; // don't repeat for more than 10 rep. paranoia
    do {
      parent = findParentTag(child);
      if (parent != null) {
        predecessor.add(parent);
        child = parent;
      }
      maxRep--;
    } while (parent != null && parent.getRootTagId() != null && maxRep > 0);
    return predecessor;
  }

  private Tag findParentTag(Tag tag) {
    return wingsPersistence.createQuery(Tag.class)
        .field("appId")
        .equal(tag.getAppId())
        .field("envId")
        .equal(tag.getEnvId())
        .field("children")
        .equal(tag.getUuid())
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#getTag(java.lang.String, java.lang.String)
   */
  @Override
  public Tag getTag(String appId, String tagId) {
    return wingsPersistence.get(Tag.class, appId, tagId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#updateTag(software.wings.beans.Tag)
   */
  @Override
  public Tag updateTag(Tag tag) {
    Builder<String, Object> mapBuilder =
        ImmutableMap.<String, Object>builder().put("name", tag.getName()).put("description", tag.getDescription());
    if (tag.getAutoTaggingRule() != null) {
      mapBuilder.put("autoTaggingRule", tag.getAutoTaggingRule());
    }
    if (tag.getChildren() != null) {
      mapBuilder.put("children", tag.getChildren());
    }
    wingsPersistence.updateFields(Tag.class, tag.getUuid(), mapBuilder.build());

    return wingsPersistence.get(Tag.class, tag.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#deleteTag(java.lang.String, java.lang.String)
   */
  @Override
  public void deleteTag(String appId, String tagId) {
    Tag tag = wingsPersistence.get(Tag.class, tagId);
    if (tag != null && !tag.isRootTag()) {
      cascadingDelete(tag);
    }
  }

  private void cascadingDelete(Tag tag) {
    if (tag != null) {
      wingsPersistence.delete(Tag.class, tag.getUuid());
      if (tag.getChildren() != null) {
        tag.getChildren().forEach(this ::cascadingDelete);
      } else { // leaf tag should update hostInstance mapping
        updateAllServiceTemplatesWithDeletedHosts(tag);
      }
    }
  }

  @Override
  public void deleteHostFromTags(List<Tag> tags, Host host) {
    tags.stream()
        .map(serviceTemplateService::getTemplatesByLeafTag)
        .flatMap(List::stream)
        .forEach(
            serviceTemplate -> serviceInstanceService.updateInstanceMappings(serviceTemplate, asList(), asList(host)));
  }

  private void updateAllServiceTemplatesWithDeletedHosts(Tag tag) {
    List<Host> hosts = hostService.getHostsByTags(tag.getAppId(), tag.getEnvId(), asList(tag));
    List<ServiceTemplate> serviceTemplates = serviceTemplateService.getTemplatesByLeafTag(tag);
    if (serviceTemplates != null) {
      serviceTemplates.forEach(
          serviceTemplate -> serviceInstanceService.updateInstanceMappings(serviceTemplate, new ArrayList<>(), hosts));
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#getRootConfigTag(java.lang.String, java.lang.String)
   */
  @Override
  public Tag getRootConfigTag(String appId, String envId) {
    return wingsPersistence.createQuery(Tag.class).field("envId").equal(envId).field("rootTag").equal(true).get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#tagHosts(java.lang.String, java.lang.String, java.util.List)
   */
  @Override
  public void tagHosts(String appId, String tagId, List<String> hostIds) {
    Tag tag = wingsPersistence.get(Tag.class, appId, tagId);
    List<ServiceTemplate> serviceTemplates = serviceTemplateService.getTemplatesByLeafTag(tag);

    if (hostIds == null || tag == null) {
      return;
    }

    List<Host> inputHosts =
        hostService.getHostsByHostIds(appId, hostService.getInfraId(appId, tag.getEnvId()), hostIds);
    List<Host> existingMappedHosts = hostService.getHostsByTags(appId, tag.getEnvId(), asList(tag));
    List<Host> hostToBeUntagged =
        existingMappedHosts.stream().filter(host -> !inputHosts.contains(host)).collect(toList());
    List<Host> hostTobeTagged =
        inputHosts.stream().filter(host -> !existingMappedHosts.contains(host)).collect(toList());

    // Tag
    hostTobeTagged.forEach(host -> {
      List<Tag> tags = removeAnyTagFromSameTagTree(tag, host);
      hostService.setTags(host, tags);
      if (serviceTemplates != null) {
        serviceTemplates.forEach(
            serviceTemplate -> serviceInstanceService.updateInstanceMappings(serviceTemplate, asList(host), asList()));
      }
    });

    // Un-tag
    hostToBeUntagged.forEach(host -> {
      hostService.removeTagFromHost(host, tag);
      if (serviceTemplates != null) {
        serviceTemplates.forEach(
            serviceTemplate -> serviceInstanceService.updateInstanceMappings(serviceTemplate, asList(), asList(host)));
      }
    });
  }

  private List<Tag> removeAnyTagFromSameTagTree(Tag tag, Host host) {
    List<Tag> tags = host.getTags();
    if (tags != null && tags.size() != 0) {
      for (int i = 0; i < tags.size(); i++) {
        if (tag.getRootTagId().equals(tags.get(i).getRootTagId())) {
          tags.remove(i);
          break;
        }
      }
    }
    tags.add(tag);
    return tags;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#getTagsByName(java.lang.String, java.lang.String, java.util.List)
   */
  @Override
  public List<Tag> getTagsByName(String appId, String envId, List<String> tagNames) {
    List<Tag> tags = new ArrayList<>();
    if (tagNames != null && tagNames.size() > 0) {
      tagNames.forEach(name -> {
        Tag tag = wingsPersistence.createQuery(Tag.class).field("envId").equal(envId).field("name").equal(name).get();
        if (tag != null) {
          tags.add(tag);
        }
      });
    }
    return tags;
  }

  @Override
  public List<Tag> getLeafTags(Tag root) {
    Query<Tag> q = wingsPersistence.createQuery(Tag.class).field("rootTagId").equal(root.getUuid());
    q.or(wingsPersistence.createQuery(Tag.class).criteria("children").doesNotExist(),
        wingsPersistence.createQuery(Tag.class).criteria("children").sizeEq(0));
    return q.asList();
  }
}
