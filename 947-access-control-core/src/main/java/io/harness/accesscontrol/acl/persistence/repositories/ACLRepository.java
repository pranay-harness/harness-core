package io.harness.accesscontrol.acl.persistence.repositories;

import io.harness.accesscontrol.acl.persistence.ACL;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Set;

@OwnedBy(HarnessTeam.PL)
public interface ACLRepository {
  long insertAllIgnoringDuplicates(List<ACL> acls);

  long deleteByRoleAssignmentId(String id);

  List<String> getDistinctResourceSelectorsInACLs(String roleAssignmentId);

  long deleteByRoleAssignmentIdAndResourceSelectors(String roleAssignmentId, Set<String> resourceSelectorsToDelete);

  long deleteByRoleAssignmentIdAndPermissions(String roleAssignmentId, Set<String> permissions);

  long deleteByRoleAssignmentIdAndPrincipals(String roleAssignmentId, Set<String> principals);

  List<String> getDistinctPermissionsInACLsForRoleAssignment(String roleAssignmentId);

  List<String> getDistinctPrincipalsInACLsForRoleAssignment(String id);

  Set<String> getByAclQueryStringInAndEnabled(List<String> aclQueryStrings, boolean enabled);

  void cleanCollection();

  void renameCollection(String newCollectionName);
}
