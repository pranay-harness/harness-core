package software.wings.service.intfc;

import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;

/**
 * Created by anubhaw on 4/25/16.
 */
public interface TagService {
  PageResponse<Tag> listRootTags(PageRequest<Tag> request);

  Tag saveTag(String parentTagId, Tag tag);

  Tag getTag(String appId, String tagId);

  Tag updateTag(Tag tag);

  void deleteTag(String appId, String tagId);

  Tag getRootConfigTag(String appId, String envId);

  void tagHosts(String appId, String tagId, List<String> hostIds);

  List<Tag> getTagsByName(String appId, String envId, List<String> tagNames);
}
