package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileScopingRule;

import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;

public interface DelegateProfileService extends OwnedByAccount {
  PageResponse<DelegateProfile> list(PageRequest<DelegateProfile> pageRequest);
  DelegateProfile get(String accountId, String delegateProfileId);
  DelegateProfile fetchPrimaryProfile(String accountId);
  DelegateProfile update(DelegateProfile delegateProfile);
  DelegateProfile updateScopingRules(
      String accountId, String delegateProfileId, List<DelegateProfileScopingRule> scopingRules);
  DelegateProfile add(DelegateProfile delegateProfile);
  void delete(String accountId, String delegateProfileId);
  DelegateProfile updateDelegateProfileSelectors(String delegateProfileId, String accountId, List<String> selectors);
}
