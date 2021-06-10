package io.harness.accesscontrol.acl.repository;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Set;

@OwnedBy(PL)
public interface ACLRepositoryCustom {
  long insertAllIgnoringDuplicates(List<ACL> acls);

  List<ACL> findByUserGroup(String scopeIdentifier, String userGroupIdentifier);

  List<ACL> findByRole(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> findByResourceGroup(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> getByRoleAssignmentId(String id);

  long deleteByRoleAssignmentId(String id);

  List<String> getDistinctResourceSelectorsInACLs(String roleAssignmentId);

  long deleteByRoleAssignmentIdAndResourceSelectors(String roleAssignmentId, Set<String> resourceSelectorsToDelete);

  long deleteByRoleAssignmentIdAndPermissions(String roleAssignmentId, Set<String> permissions);

  long deleteByRoleAssignmentIdAndPrincipals(String roleAssignmentId, Set<String> principals);

  List<String> getDistinctPermissionsInACLsForRoleAssignment(String roleAssignmentId);

  List<String> getDistinctPrincipalsInACLsForRoleAssignment(String id);

  Set<String> getByAclQueryStringInAndEnabled(List<String> aclQueries, boolean enabled);
}
