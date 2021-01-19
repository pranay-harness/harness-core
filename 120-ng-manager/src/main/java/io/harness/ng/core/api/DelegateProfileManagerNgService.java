package io.harness.ng.core.api;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateProfileDetailsNg;
import io.harness.delegate.beans.ScopingRuleDetailsNg;

import java.util.List;

public interface DelegateProfileManagerNgService {
  PageResponse<DelegateProfileDetailsNg> list(String accountId, PageRequest<DelegateProfileDetailsNg> pageRequest);

  DelegateProfileDetailsNg get(String accountId, String delegateProfileId);

  DelegateProfileDetailsNg update(DelegateProfileDetailsNg delegateProfile);

  DelegateProfileDetailsNg updateScopingRules(
      String accountId, String delegateProfileId, List<ScopingRuleDetailsNg> scopingRules);

  DelegateProfileDetailsNg updateSelectors(String accountId, String delegateProfileId, List<String> selectors);

  DelegateProfileDetailsNg add(DelegateProfileDetailsNg delegateProfile);

  void delete(String accountId, String delegateProfileId);
}
