package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ApiKeyEntry;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

public interface ApiKeyService {
  PageResponse<ApiKeyEntry> list(PageRequest<ApiKeyEntry> request);
  String generate(@NotEmpty String accountId);
  void delete(@NotEmpty String uuid);
  String get(@NotEmpty String uuid, @NotEmpty String accountId);
  void validate(@NotEmpty String key, @NotEmpty String accountId);
}
