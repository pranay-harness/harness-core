package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Preference;

public interface PreferenceService {
  Preference save(String accountId, String userId, Preference preference);
  PageResponse<Preference> list(PageRequest<Preference> pageRequest, @NotEmpty String userId);
  Preference get(String accountId, String userId, String preferenceId);
  Preference update(String accountId, String userId, String preferenceId, Preference preference);
  void delete(@NotEmpty String accountId, @NotEmpty String userId, @NotEmpty String preferenceId);
}
