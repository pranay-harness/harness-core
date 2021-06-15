package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileScopingRule;

import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;

@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@OwnedBy(DEL)
@BreakDependencyOn("software.wings.service.intfc.ownership.OwnedByAccount")
public interface DelegateProfileService extends OwnedByAccount {
  PageResponse<DelegateProfile> list(PageRequest<DelegateProfile> pageRequest);
  DelegateProfile get(String accountId, String delegateProfileId);
  DelegateProfile fetchCgPrimaryProfile(String accountId);

  DelegateProfile fetchNgPrimaryProfile(String accountId, DelegateEntityOwner owner);

  DelegateProfile update(DelegateProfile delegateProfile);
  DelegateProfile updateScopingRules(
      String accountId, String delegateProfileId, List<DelegateProfileScopingRule> scopingRules);
  DelegateProfile add(DelegateProfile delegateProfile);
  void delete(String accountId, String delegateProfileId);
  DelegateProfile updateDelegateProfileSelectors(String delegateProfileId, String accountId, List<String> selectors);
  List<String> getDelegatesForProfile(String accountId, String profileId);
}
