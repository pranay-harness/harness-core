package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Tag;

/**
 * Created by anubhaw on 4/25/16.
 */
public interface TagService {
  PageResponse<Tag> listRootTags(PageRequest<Tag> request);

  Tag saveTag(String parentTagId, Tag tag);

  Tag getTag(String appId, String tagId);

  Tag updateTag(Tag tag);

  void deleteTag(String appId, String tagId);

  Tag linkTags(String appId, String tagId, String childTagId);

  Tag getRootConfigTag(String appId, String envId);

  Tag createAndLinkTag(String parentTagId, Tag tag);
}
