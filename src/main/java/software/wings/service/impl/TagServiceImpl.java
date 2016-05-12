package software.wings.service.impl;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.TagType.HierarchyTagName;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.ConfigFile;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Tag;
import software.wings.beans.TagType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.TagService;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 4/25/16.
 */
public class TagServiceImpl implements TagService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private ConfigService configService;

  @Override
  public PageResponse<Tag> listTag(String tagTypeId, PageRequest<Tag> request) {
    TagType tagType = wingsPersistence.get(TagType.class, tagTypeId);
    request.addFilter("tagType", tagType, EQ);
    return wingsPersistence.query(Tag.class, request);
  }

  @Override
  public Tag saveTag(Tag tag) {
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
            tag.getAutoTaggingRule(), "linkedTags", tag.getLinkedTags()));
    return wingsPersistence.get(Tag.class, tag.getUuid());
  }

  @Override
  public void deleteTag(String appId, String tagId) {
    wingsPersistence.delete(Tag.class, tagId);
  }

  @Override
  public Tag linkTags(String appId, String tagId, String childTagId) {
    Tag childTag = wingsPersistence.get(Tag.class, childTagId);
    wingsPersistence.addToList(Tag.class, tagId, "linkedTags", childTag);
    return wingsPersistence.get(Tag.class, tagId);
  }

  @Override
  public PageResponse<TagType> listTagType(PageRequest<TagType> request) {
    return wingsPersistence.query(TagType.class, request);
  }

  @Override
  public TagType getTagType(String appId, String tagTypeId) {
    return wingsPersistence.get(TagType.class, tagTypeId);
  }

  @Override
  public TagType saveTagType(TagType tagType) {
    return wingsPersistence.saveAndGet(TagType.class, tagType);
  }

  @Override
  public TagType updateTagType(TagType tagType) {
    wingsPersistence.updateFields(TagType.class, tagType.getUuid(), ImmutableMap.of("name", tagType.getName()));
    return wingsPersistence.get(TagType.class, tagType.getUuid());
  }

  @Override
  public void deleteTagType(String appId, String tagTypeId) {
    wingsPersistence.delete(TagType.class, tagTypeId);
  }

  @Override
  public String saveFile(String tagId, ConfigFile configFile, InputStream inputStream) {
    fileService.saveFile(configFile, inputStream, CONFIGS);
    String configFileId = wingsPersistence.save(configFile);
    wingsPersistence.addToList(Tag.class, tagId, "configFiles", configFile);
    return configFileId;
  }

  @Override
  public List<Tag> getRootConfigTags(String envId) {
    TagType tagType = wingsPersistence.createQuery(TagType.class).field("name").equal(HierarchyTagName).get();
    List<Tag> tags = wingsPersistence.createQuery(Tag.class).field("tagType").equal(tagType).asList();
    filterRootTags(tags);
    return tags;
  }

  private void filterRootTags(List<Tag> tags) {
    Set<Tag> childTags = new HashSet<>();
    for (Tag tag : tags) {
      childTags.addAll(tag.getLinkedTags().stream().collect(Collectors.toList()));
    }
    childTags.forEach(tags::remove);
  }
}
