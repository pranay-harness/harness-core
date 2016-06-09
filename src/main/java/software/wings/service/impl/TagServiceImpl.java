package software.wings.service.impl;

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Host;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
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
      return tag;
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.TagService#getTag(java.lang.String, java.lang.String)
   */
  @Override
  public Tag getTag(String appId, String tagId) {
    return wingsPersistence.get(Tag.class, tagId);
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

  private void updateAllServiceTemplatesWithDeletedHosts(Tag tag) {
    List<Host> hosts = hostService.getHostsByTags(tag.getAppId(), asList(tag));
    Query<ServiceTemplate> serviceTemplates =
        wingsPersistence.createQuery(ServiceTemplate.class).field("tags").hasThisElement(tag);
    if (serviceTemplates != null) {
      serviceTemplates.forEach(
          serviceTemplate -> { serviceInstanceService.updateHostMappings(serviceTemplate, new ArrayList<>(), hosts); });
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
    Tag tag = wingsPersistence.get(Tag.class, tagId);

    if (hostIds == null || tag == null) {
      return;
    }

    List<Host> hosts = wingsPersistence.createQuery(Host.class).field("tags").equal(tag.getUuid()).asList();

    // Tag hosts
    hostIds.forEach(hostId -> {
      Host host = wingsPersistence.get(Host.class, hostId);
      List<Tag> tags = getUpdateHostTagsList(tag, host);
      UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).set("tags", tags);
      wingsPersistence.update(host, updateOp);
      hosts.remove(host); // remove updated tags from all tagged hosts
    });

    // Un-tag hosts
    hosts.forEach(host -> {
      UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).removeAll("tags", tag);
      wingsPersistence.update(host, updateOp);
    });
  }

  private List<Tag> getUpdateHostTagsList(Tag tag, Host host) {
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
