package io.harness.accesscontrol.acl.repository;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(PL)
public interface ACLRepositoryCustom {
  long insertAllIgnoringDuplicates(List<ACL> acls);

  List<ACL> findByUserGroup(String scopeIdentifier, String userGroupIdentifier);

  List<ACL> findByRole(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> findByResourceGroup(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> getByRoleAssignmentId(String id);

  long deleteByRoleAssignmentId(String id);
}
