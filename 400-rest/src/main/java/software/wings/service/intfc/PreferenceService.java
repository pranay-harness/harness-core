package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.beans.AuditPreferenceResponse;
import software.wings.beans.Preference;

import org.hibernate.validator.constraints.NotEmpty;

public interface PreferenceService {
  Preference save(String accountId, String userId, Preference preference);
  PageResponse<Preference> list(PageRequest<Preference> pageRequest, @NotEmpty String userId);
  Preference get(String accountId, String userId, String preferenceId);
  Preference getPreferenceByName(String accountId, String userId, String name);
  Preference update(String accountId, String userId, String preferenceId, Preference preference);
  void delete(@NotEmpty String accountId, @NotEmpty String userId, @NotEmpty String preferenceId);
  AuditPreferenceResponse listAuditPreferences(String accountId, String userId);
}
