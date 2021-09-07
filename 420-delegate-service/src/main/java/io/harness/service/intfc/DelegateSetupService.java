package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateGroupDetails;
import io.harness.delegate.beans.DelegateGroupListing;
import io.harness.delegate.filter.DelegateFilterPropertiesDTO;

import software.wings.beans.SelectorType;

import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateSetupService {
  long getDelegateGroupCount(String accountId, String orgId, String projectId);

  DelegateGroupListing listDelegateGroupDetails(String accountId, String orgId, String projectId);

  DelegateGroupListing listDelegateGroupDetailsUpTheHierarchy(String accountId, String orgId, String projectId);

  DelegateGroupDetails getDelegateGroupDetails(String accountId, String delegateGroupId);

  DelegateGroupDetails getDelegateGroupDetailsV2(String accountId, String orgId, String projectId, String identifier);

  String getHostNameForGroupedDelegate(String hostname);

  Map<String, SelectorType> retrieveDelegateImplicitSelectors(Delegate delegate);

  Map<String, SelectorType> retrieveDelegateGroupImplicitSelectors(DelegateGroup delegateGroup);

  List<Boolean> validateDelegateGroups(String accountId, String orgId, String projectId, List<String> identifiers);

  List<Boolean> validateDelegateConfigurations(
      String accountId, String orgId, String projectId, List<String> identifiers);

  DelegateGroupDetails updateDelegateGroup(
      String accountId, String delegateGroupId, DelegateGroupDetails delegateGroupDetails);

  DelegateGroupDetails updateDelegateGroup(
      String accountId, String orgId, String projectId, String identifier, DelegateGroupDetails delegateGroupDetails);

  DelegateGroupListing listDelegateGroupDetailsV2(String accountId, String orgId, String projectId,
      String filterIdentifier, String searchTerm, DelegateFilterPropertiesDTO delegateFilterPropertiesDTO,
      PageRequest<DelegateGroupDetails> pageRequest);
}
