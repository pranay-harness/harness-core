package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCodes.INVALID_REQUEST;
import static software.wings.beans.Tag.Builder.aTag;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.beans.Tag.TagType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 4/25/16.
 */
@ValidateOnExecution
@Singleton
public class TagServiceImpl implements TagService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private HostService hostService;
  @Inject private InfrastructureService infrastructureService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ExecutorService executorService;
  @Inject private ConfigService configService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Tag> list(PageRequest<Tag> request) {
    return wingsPersistence.query(Tag.class, request);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#save(java.lang.String, software.wings.beans.Tag)
   */
  @Override
  public Tag save(String parentTagId, Tag tag) {
    Tag parentTag = wingsPersistence.get(Tag.class, tag.getAppId(), parentTagId);
    tag.setTagType(TagType.TAGGED_HOST);
    return saveInternal(parentTag, tag);
  }

  private Tag saveInternal(Tag parentTag, Tag tag) {
    if (parentTag == null && !tag.getTagType().equals(TagType.ENVIRONMENT)) {
      throw new WingsException(INVALID_ARGUMENT, "message", "Parent tag couldn't be found.");
    } else if (parentTag == null && tag.getTagType().equals(TagType.ENVIRONMENT)) {
      return wingsPersistence.saveAndGet(Tag.class, tag);
    } else if (parentTag.getTagType().equals(TagType.UNTAGGED_HOST)) {
      throw new WingsException(
          INVALID_REQUEST, "message", "Environment default tag for untagged hosts can not be extended");
    } else {
      tag.setParentTagId(parentTag.getUuid());
      tag.setRootTagId(
          parentTag.getTagType().equals(TagType.ENVIRONMENT) ? parentTag.getUuid() : parentTag.getRootTagId());
      tag = wingsPersistence.saveAndGet(Tag.class, tag);
      wingsPersistence.addToList(Tag.class, tag.getAppId(), parentTag.getUuid(), "children", tag.getUuid());
      updateServiceTemplateWithCommandPredecessor(tag);
      return tag;
    }
  }

  @Override
  public void createDefaultRootTagForEnvironment(Environment env) {
    Tag envTag = saveInternal(null,
        aTag()
            .withAppId(env.getAppId())
            .withEnvId(env.getUuid())
            .withName(env.getName())
            .withDescription(env.getName())
            .withTagType(TagType.ENVIRONMENT)
            .build());
    saveInternal(envTag,
        aTag()
            .withAppId(env.getAppId())
            .withEnvId(env.getUuid())
            .withParentTagId(envTag.getUuid())
            .withName(env.getName() + "-Untagged-Hosts")
            .withDescription("Hosts with no tags will appear here")
            .withTagType(TagType.UNTAGGED_HOST)
            .build());
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
   * @see software.wings.service.intfc.TagService#get(java.lang.String, java.lang.String)
   */
  @Override
  public Tag get(String appId, String envId, String tagId) {
    return wingsPersistence.createQuery(Tag.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field(ID_KEY)
        .equal(tagId)
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#update(software.wings.beans.Tag)
   */
  @Override
  public Tag update(Tag tag) {
    tag = get(tag.getAppId(), tag.getEnvId(), tag.getUuid());
    if (tag == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Tag doesn't exist");
    }

    if (!tag.getTagType().isModificationAllowed()) {
      throw new WingsException(INVALID_REQUEST, "message", "System generated Tags can not be modified by users");
    }

    Builder<String, Object> mapBuilder =
        ImmutableMap.<String, Object>builder().put("name", tag.getName()).put("description", tag.getDescription());
    if (tag.getAutoTaggingRule() != null) {
      mapBuilder.put("autoTaggingRule", tag.getAutoTaggingRule());
    }
    wingsPersistence.updateFields(Tag.class, tag.getUuid(), mapBuilder.build());
    return wingsPersistence.get(Tag.class, tag.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#delete(java.lang.String, java.lang.String)
   */
  @Override
  public void delete(String appId, String envId, String tagId) {
    Tag tag = get(appId, envId, tagId);
    Validator.notNullCheck("Tag", tag);
    if (tag.getTagType().isModificationAllowed()) {
      cascadingDelete(tag);
    }
  }

  private void cascadingDelete(Tag tag) {
    if (tag != null) {
      deleteTag(tag);
      if (tag.getChildren() != null && tag.getChildren().size() > 0) {
        tag.getChildren().forEach(this ::cascadingDelete);
      } else { // leaf tag should update hostInstance mapping
        executorService.submit(() -> updateAllServiceTemplatesWithDeletedHosts(tag));
      }
    }
  }

  private void deleteTag(Tag tag) {
    wingsPersistence.delete(tag);
    executorService.submit(() -> configService.deleteByEntityId(tag.getAppId(), tag.getUuid()));
  }

  @Override
  public void deleteByEnv(String appId, String envId) {
    wingsPersistence.createQuery(Tag.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field("tagType")
        .equal(TagType.ENVIRONMENT)
        .asList()
        .forEach(this ::cascadingDelete);
  }

  @Override
  public List<Tag> flattenTagTree(String appId, String envId, String tagId) {
    Tag tag = isNullOrEmpty(tagId) ? getRootConfigTag(appId, envId) : get(appId, envId, tagId);
    List<Tag> flattenTreeNodes = new ArrayList<>();
    collectTreeNodes(tag, flattenTreeNodes);
    return flattenTreeNodes;
  }

  @Override
  public String getTagHierarchyPathString(Tag tag) {
    String path = tag.getName().trim();
    int maxDepth = 10;
    while (maxDepth > 0 && !isNullOrEmpty(tag.getParentTagId())) {
      maxDepth--;
      tag = get(tag.getAppId(), tag.getEnvId(), tag.getParentTagId());
      path = tag.getName().trim() + "/" + path;
    }
    return path;
  }

  @Override
  public Tag getDefaultTagForUntaggedHosts(String appId, String envId) {
    return wingsPersistence.createQuery(Tag.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field("tagType")
        .equal(TagType.UNTAGGED_HOST)
        .get();
  }

  private void collectTreeNodes(Tag tag, List<Tag> flattenTree) {
    if (tag != null) {
      flattenTree.add(tag);
      if (tag.getChildren() != null && tag.getChildren().size() > 0) {
        tag.getChildren().forEach(child -> collectTreeNodes(child, flattenTree));
      }
    }
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
    return wingsPersistence.createQuery(Tag.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field("tagType")
        .equal(TagType.ENVIRONMENT)
        .get();
  }

  @Override
  public void tagHosts(Tag tag, List<Host> inputHosts) {
    List<Host> existingMappedHosts = hostService.getHostsByTags(tag.getAppId(), tag.getEnvId(), asList(tag));
    List<Host> hostToBeUntagged =
        existingMappedHosts.stream().filter(host -> !inputHosts.contains(host)).collect(toList());
    List<Host> hostTobeTagged =
        inputHosts.stream().filter(host -> !existingMappedHosts.contains(host)).collect(toList());

    tagHosts(tag, hostToBeUntagged, hostTobeTagged);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#tagHosts(java.lang.String, java.lang.String, java.util.List)
   */
  @Override
  public void tagHostsByApi(String appId, String envId, String tagId, List<String> hostIds) {
    Tag tag = get(appId, envId, tagId);
    Validator.notNullCheck("Tag", tag);

    if (!tag.getTagType().isModificationAllowed()) {
      throw new WingsException(INVALID_REQUEST, "message", "System generated Tags can not be modified by users");
    }

    List<Host> inputHosts =
        hostService.getHostsByHostIds(appId, infrastructureService.getInfraByEnvId(tag.getEnvId()).getUuid(), hostIds);
    tagHosts(tag, inputHosts);
  }

  private void tagHosts(Tag tag, List<Host> hostToBeUntagged, List<Host> hostTobeTagged) {
    List<ServiceTemplate> serviceTemplates = serviceTemplateService.getTemplatesByLeafTag(tag);

    // Tag
    hostTobeTagged.forEach(host -> {
      hostService.setTag(host, tag);
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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#getTagsByName(java.lang.String, java.lang.String, java.util.List)
   */
  @Override
  public List<Tag> getTagsByName(String appId, String envId, List<String> tagNames) {
    return tagNames.stream()
        .map(tagName
            -> wingsPersistence.createQuery(Tag.class)
                   .field("appId")
                   .equal(appId)
                   .field("envId")
                   .equal(envId)
                   .field("name")
                   .equal(tagName)
                   .get())
        .filter(Objects::nonNull)
        .collect(toList());
  }

  @Override
  public List<Tag> getUserCreatedLeafTags(String appId, String envId) {
    return getLeafTagInSubTree(getRootConfigTag(appId, envId))
        .stream()
        .filter(tag -> tag.getTagType().equals(TagType.TAGGED_HOST))
        .collect(toList());
  }

  @Override
  public List<Tag> getLeafTagInSubTree(Tag tag) {
    List<Tag> leafTags = new ArrayList<>();
    getLeafTagInSubTree(tag, leafTags);
    return leafTags;
  }

  private void getLeafTagInSubTree(Tag tag, List<Tag> leafTags) {
    if (tag != null) {
      if (tag.getChildren() != null && tag.getChildren().size() > 0) {
        tag.getChildren().forEach(child -> getLeafTagInSubTree(child, leafTags));
      } else {
        leafTags.add(tag);
      }
    }
  }
}
