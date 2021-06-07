package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateGroupDetails;
import io.harness.delegate.beans.DelegateGroupListing;

import software.wings.beans.SelectorType;

import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateSetupService {
  DelegateGroupListing listDelegateGroupDetails(String accountId, String orgId, String projectId);

  DelegateGroupDetails getDelegateGroupDetails(String accountId, String delegateGroupId);

  String getHostNameForGroupedDelegate(String hostname);

  Map<String, SelectorType> retrieveDelegateImplicitSelectors(Delegate delegate);

  List<Boolean> validateDelegateGroups(String accountId, String orgId, String projectId, List<String> identifiers);

  List<Boolean> validateDelegateConfigurations(
      String accountId, String orgId, String projectId, List<String> identifiers);
}
