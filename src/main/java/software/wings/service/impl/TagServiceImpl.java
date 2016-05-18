package software.wings.service.impl;

import com.google.common.collect.ImmutableMap;

import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Host;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.TagService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 4/25/16.
 */
public class TagServiceImpl implements TagService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Tag> listRootTags(PageRequest<Tag> request) {
    return wingsPersistence.query(Tag.class, request);
  }

  @Override
  public Tag saveTag(String parentTagId, Tag tag) {
    return wingsPersistence.saveAndGet(Tag.class, tag);
  }

  @Override
  public Tag getTag(String appId, String tagId) {
    return wingsPersistence.get(Tag.class, tagId);
  }

  @Override
  public Tag updateTag(Tag tag) {
    wingsPersistence.updateFields(Tag.class, tag.getUuid(),
        ImmutableMap.of("name", tag.getName(), "description", tag.getDescription(), "autoTaggingRule",
            tag.getAutoTaggingRule(), "children", tag.getChildren()));
    return wingsPersistence.get(Tag.class, tag.getUuid());
  }

  @Override
  public void deleteTag(String appId, String tagId) {
    Tag tag = wingsPersistence.get(Tag.class, tagId);
    if (tag != null && !tag.isRootTag()) {
      wingsPersistence.delete(Tag.class, tagId);
    }
  }

  @Override
  public Tag linkTags(String appId, String tagId, String childTagId) {
    Tag childTag = wingsPersistence.get(Tag.class, childTagId);
    wingsPersistence.addToList(Tag.class, tagId, "children", childTag);
    return wingsPersistence.get(Tag.class, tagId);
  }

  @Override
  public Tag getRootConfigTag(String appId, String envId) {
    return wingsPersistence.createQuery(Tag.class).field("envId").equal(envId).field("rootTag").equal(true).get();
  }

  @Override
  public Tag createAndLinkTag(String parentTagId, Tag tag) {
    Tag parentTag = wingsPersistence.get(Tag.class, parentTagId);
    tag = wingsPersistence.saveAndGet(Tag.class, tag);
    if (parentTag != null) {
      wingsPersistence.addToList(Tag.class, parentTagId, "children", tag.getUuid());
    }
    return tag;
  }

  @Override
  public void tagHosts(String appId, String tagId, List<String> hostIds) {
    Tag tag = wingsPersistence.get(Tag.class, tagId);
    if (hostIds != null) {
      hostIds.forEach(hostId -> {
        Host host = wingsPersistence.get(Host.class, hostId);
        UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).add("tags", tag);
        wingsPersistence.update(host, updateOp);
      });
    }
  }

  @Override
  public void untagHosts(String appId, String tagId, List<String> hostIds) {
    Tag tag = wingsPersistence.get(Tag.class, tagId);
    if (hostIds != null) {
      hostIds.forEach(hostId -> {
        Host host = wingsPersistence.get(Host.class, hostId);
        UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).removeAll("tags", tag);
        wingsPersistence.update(host, updateOp);
      });
    }
  }
}
