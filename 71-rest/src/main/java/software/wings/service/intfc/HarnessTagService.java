package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagLink;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface HarnessTagService {
  HarnessTag create(HarnessTag tag);
  HarnessTag update(HarnessTag tag);
  HarnessTag get(@NotBlank String accountId, @NotBlank String key);
  HarnessTag getTagWithInUseValues(@NotBlank String accountId, @NotBlank String key);
  PageResponse<HarnessTag> list(PageRequest<HarnessTag> request);
  void delete(@NotBlank String accountId, @NotBlank String key);
  void delete(@NotNull HarnessTag tag);
  void attachTag(HarnessTagLink tagLink);
  void detachTag(@NotBlank String accountId, @NotBlank String entityId, @NotBlank String key);
  PageResponse<HarnessTagLink> listResourcesWithTag(String accountId, PageRequest<HarnessTagLink> request);
  void pruneTagLinks(String accountId, String entityId);
  void authorizeTagAttachDetach(String appId, HarnessTagLink tagLink);
  List<HarnessTagLink> getTagLinksWithEntityId(String accountId, String entityId);
  void pushTagLinkToGit(String accountId, String entityId, boolean syncFromGit);
  void attachTagWithoutGitPush(HarnessTagLink tagLink);
  void detachTagWithoutGitPush(@NotBlank String accountId, @NotBlank String entityId, @NotBlank String key);

  PageResponse<HarnessTag> listTagsWithInUseValues(PageRequest<HarnessTag> request);
  List<HarnessTag> listTags(String accountId);
  HarnessTag createTag(HarnessTag tag, boolean syncFromGit);
  HarnessTag updateTag(HarnessTag tag, boolean syncFromGit);
  void deleteTag(@NotBlank String accountId, @NotBlank String key, boolean syncFromGit);
}
