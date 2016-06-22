package software.wings.service.intfc;

import software.wings.beans.Host;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/25/16.
 */
public interface TagService {
  /**
   * List root tags.
   *
   * @param request the request
   * @return the page response
   */
  PageResponse<Tag> listRootTags(PageRequest<Tag> request);

  /**
   * Save tag.
   *
   * @param parentTagId the parent tag id
   * @param tag         the tag
   * @return the tag
   */
  Tag saveTag(String parentTagId, Tag tag);

  /**
   * Gets the tag.
   *
   * @param appId the app id
   * @param tagId the tag id
   * @return the tag
   */
  Tag getTag(String appId, String tagId);

  /**
   * Update tag.
   *
   * @param tag the tag
   * @return the tag
   */
  Tag updateTag(Tag tag);

  /**
   * Delete tag.
   *
   * @param appId the app id
   * @param tagId the tag id
   */
  void deleteTag(String appId, String tagId);

  /**
   * Gets the root config tag.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the root config tag
   */
  Tag getRootConfigTag(String appId, String envId);

  /**
   * Tag hosts.
   *
   * @param appId   the app id
   * @param tagId   the tag id
   * @param hostIds the host ids
   */
  void tagHosts(String appId, String tagId, List<String> hostIds);

  /**
   * Gets the tags by name.
   *
   * @param appId    the app id
   * @param envId    the env id
   * @param tagNames the tag names
   * @return the tags by name
   */
  List<Tag> getTagsByName(String appId, String envId, List<String> tagNames);

  /**
   * Gets leaf tags.
   *
   * @param root the root
   * @return the leaf tags
   */
  List<Tag> getLeafTags(Tag root);

  void deleteHostFromTags(Host host);
}
