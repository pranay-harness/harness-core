package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotBlank;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagLink;
import software.wings.graphql.schema.type.aggregation.QLEntityType;
import software.wings.security.PermissionAttribute.Action;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface HarnessTagService {
  HarnessTag create(HarnessTag tag);
  HarnessTag update(HarnessTag tag);
  HarnessTag get(@NotBlank String accountId, @NotBlank String key);
  HarnessTag getTagWithInUseValues(@NotBlank String accountId, @NotBlank String key);
  PageResponse<HarnessTag> list(PageRequest<HarnessTag> request);
  void delete(@NotBlank String accountId, @NotBlank String key);
  void delete(@NotNull HarnessTag tag);
  @ValidationGroups(Update.class) void attachTag(@Valid HarnessTagLink tagLink);
  @ValidationGroups(Update.class) void detachTag(@Valid HarnessTagLink tagLink);
  PageResponse<HarnessTagLink> listResourcesWithTag(String accountId, PageRequest<HarnessTagLink> request);
  void pruneTagLinks(String accountId, String entityId);
  @ValidationGroups(Update.class) void authorizeTagAttachDetach(String appId, @Valid HarnessTagLink tagLink);
  List<HarnessTagLink> getTagLinksWithEntityId(String accountId, String entityId);
  void pushTagLinkToGit(String accountId, String appId, String entityId, EntityType entityType, boolean syncFromGit);
  void attachTagWithoutGitPush(HarnessTagLink tagLink);
  void detachTagWithoutGitPush(@NotBlank String accountId, @NotBlank String entityId, @NotBlank String key);

  PageResponse<HarnessTag> listTagsWithInUseValues(PageRequest<HarnessTag> request);
  List<HarnessTag> listTags(String accountId);
  HarnessTag createTag(HarnessTag tag, boolean syncFromGit, boolean allowSystemTagCreate);
  HarnessTag updateTag(HarnessTag tag, boolean syncFromGit);
  void deleteTag(@NotBlank String accountId, @NotBlank String key, boolean syncFromGit);

  void validateTagResourceAccess(String appId, String accountId, String entityId, EntityType entityType, Action action);

  Set<HarnessTagLink> getTagLinks(String accountId, EntityType entityType, List<String> entityIds, String key);

  /**
   * @param accountId
   * @param key
   * @param entityType
   * @param values if value is not present, returns all the entityIds with the key and any value
   * @return set of entityIds
   */
  Set<String> getEntityIdsWithTag(String accountId, String key, QLEntityType entityType, List<String> values);
}
