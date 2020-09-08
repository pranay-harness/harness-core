package software.wings.service.intfc;

import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.ScopingRuleDetails;

import java.util.List;

public interface DelegateProfileManagerService {
  List<DelegateProfileDetails> list(String accountId);

  DelegateProfileDetails get(String accountId, String delegateProfileId);

  DelegateProfileDetails update(DelegateProfileDetails delegateProfile);

  DelegateProfileDetails updateScopingRules(
      String accountId, String delegateProfileId, List<ScopingRuleDetails> scopingRules);

  DelegateProfileDetails updateSelectors(String accountId, String delegateProfileId, List<String> selectors);

  DelegateProfileDetails add(DelegateProfileDetails delegateProfile);

  void delete(String accountId, String delegateProfileId);
}
