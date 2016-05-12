package software.wings.service.intfc;

import software.wings.beans.ConfigFile;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Tag;
import software.wings.beans.TagType;

import java.io.InputStream;
import java.util.List;

/**
 * Created by anubhaw on 4/25/16.
 */
public interface TagService {
  PageResponse<TagType> listTagType(PageRequest<TagType> request);

  TagType getTagType(String appId, String tagTypeId);

  TagType saveTagType(TagType tagType);

  TagType updateTagType(TagType tagType);

  void deleteTagType(String appId, String tagTypeId);

  PageResponse<Tag> listTag(String tagTypeId, PageRequest<Tag> request);

  Tag saveTag(Tag tag);

  Tag getTag(String appId, String tagId);

  Tag updateTag(Tag tag);

  void deleteTag(String appId, String tagId);

  Tag linkTags(String appId, String tagId, String childTagId);

  List<Tag> getRootConfigTags(String envId);

  String saveFile(String tagId, ConfigFile configFile, InputStream inputStream);
}
