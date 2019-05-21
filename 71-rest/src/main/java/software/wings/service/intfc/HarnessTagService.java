package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagLink;

import javax.validation.constraints.NotNull;

public interface HarnessTagService {
  HarnessTag create(HarnessTag tag);
  HarnessTag update(HarnessTag tag);
  HarnessTag get(@NotBlank String accountId, @NotBlank String key);
  PageResponse<HarnessTag> list(PageRequest<HarnessTag> request);
  void delete(@NotBlank String accountId, @NotBlank String key);
  void delete(@NotNull HarnessTag tag);
  void attachTag(HarnessTagLink tagLink);
  void detachTag(@NotBlank String accountId, @NotBlank String entityId, @NotBlank String key);
  PageResponse<HarnessTagLink> listResourcesWithTag(PageRequest<HarnessTagLink> request);
}
