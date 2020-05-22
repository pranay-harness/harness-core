package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.DelegateProfile;
import software.wings.service.intfc.ownership.OwnedByAccount;

public interface DelegateProfileService extends OwnedByAccount {
  PageResponse<DelegateProfile> list(PageRequest<DelegateProfile> pageRequest);
  DelegateProfile get(String accountId, String delegateProfileId);
  DelegateProfile fetchPrimaryProfile(String accountId);
  DelegateProfile update(DelegateProfile delegateProfile);
  DelegateProfile add(DelegateProfile delegateProfile);
  void delete(String accountId, String delegateProfileId);
}
