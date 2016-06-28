package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  PageResponse<Tag> list(PageRequest<Tag> request);

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
   * @return the tag
   */
  Tag get(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String tagId);

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
  void tagHosts(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String tagId, @NotNull List<String> hostIds);

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
  List<Tag> getLeafTags(@NotNull Tag root);

  /**
   * Delete by env.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void deleteByEnv(String appId, String envId);
}
