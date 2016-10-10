package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Environment;
import software.wings.beans.Tag;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 4/25/16.
 */
public interface TagService {
  /**
   * List root tags.
   *
   * @param request the request
   * @param withTagHierarchy
   * @return the page response
   */
  PageResponse<Tag> list(PageRequest<Tag> request, boolean withTagHierarchy);

  /**
   * Save tag.
   *
   * @param parentTagId the parent tag id
   * @param tag         the tag
   * @return the tag
   */
  @ValidationGroups(Create.class) Tag save(String parentTagId, @Valid Tag tag);

  /**
   * Gets the tag.
   *
   * @param appId the app id
   * @param envId the env id
   * @param tagId the tag id
   * @param withTagHierarchy
   * @return the tag
   */
  Tag get(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String tagId, boolean withTagHierarchy);

  /**
   * Update tag.
   *
   * @param tag the tag
   * @return the tag
   */
  @ValidationGroups(Update.class) Tag update(@Valid Tag tag);

  /**
   * Delete tag.
   *
   * @param appId the app id
   * @param envId the env id
   * @param tagId the tag id
   */
  void delete(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String tagId);

  /**
   * Gets the root config tag.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the root config tag
   */
  Tag getRootConfigTag(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Tag hosts.
   *
   * @param appId   the app id
   * @param envId   the env id
   * @param tagId   the tag id
   * @param hostIds the host ids
   */
  void tagHostsByApi(
      @NotEmpty String appId, @NotEmpty String envId, @NotEmpty String tagId, @NotNull List<String> hostIds);

  /**
   * Tag hosts.
   *
   * @param tag   the tag
   * @param hosts the hosts
   */
  void tagHosts(@NotNull Tag tag, @NotNull List<ApplicationHost> hosts);

  /**
   * Gets the tags by name.
   *
   * @param appId    the app id
   * @param envId    the env id
   * @param tagNames the tag names
   * @return the tags by name
   */
  List<Tag> getTagsByName(@NotEmpty String appId, @NotEmpty String envId, @NotNull List<String> tagNames);

  /**
   * Gets leaf tags.
   *
   * @param root the root
   * @return the leaf tags
   */
  List<Tag> getLeafTagInSubTree(@NotNull Tag root);

  /**
   * Delete by env.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void deleteByEnv(String appId, String envId);

  /**
   * Create default root tag for environment.
   *
   * @param env the env
   * @return the tag
   */
  void createDefaultRootTagForEnvironment(Environment env);

  /**
   * Flatten tag tree list.
   *
   * @param appId the app id
   * @param envId the env id
   * @param tagId the tag id
   * @return the list
   */
  List<Tag> flattenTagTree(String appId, String envId, String tagId);

  /**
   * Gets tag hierarchy path string.
   *
   * @param tag the tag
   * @return the tag hierarchy path string
   */
  String getTagHierarchyPathString(Tag tag);

  /**
   * Gets default tag for untagged hosts.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the default tag for untagged hosts
   */
  Tag getDefaultTagForUntaggedHosts(String appId, String envId);

  /**
   * Gets user created leaf tags.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the user created leaf tags
   */
  List<Tag> getUserCreatedLeafTags(String appId, String envId);

  /**
   * Exist boolean.
   *
   * @param appId the app id
   * @param tagId the tag id
   * @return the boolean
   */
  boolean exist(String appId, String tagId);
}
