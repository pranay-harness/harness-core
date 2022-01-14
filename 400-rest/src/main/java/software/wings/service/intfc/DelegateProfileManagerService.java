package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.ScopingRuleDetails;

import java.util.List;

@OwnedBy(DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public interface DelegateProfileManagerService {
  PageResponse<DelegateProfileDetails> list(String accountId, PageRequest<DelegateProfileDetails> pageRequest);

  DelegateProfileDetails get(String accountId, String delegateProfileId);

  DelegateProfileDetails update(DelegateProfileDetails delegateProfile);

  DelegateProfileDetails updateScopingRules(
      String accountId, String delegateProfileId, List<ScopingRuleDetails> scopingRules);

  DelegateProfileDetails updateSelectors(String accountId, String delegateProfileId, List<String> selectors);

  DelegateProfileDetails add(DelegateProfileDetails delegateProfile);

  void delete(String accountId, String delegateProfileId);
}
