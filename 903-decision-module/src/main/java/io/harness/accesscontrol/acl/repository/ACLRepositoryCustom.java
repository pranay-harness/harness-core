package io.harness.accesscontrol.acl.repository;

import io.harness.accesscontrol.acl.models.ACL;

import java.util.List;

public interface ACLRepositoryCustom {
  long insertAllIgnoringDuplicates(List<ACL> acls);

  List<ACL> findByRole(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> findByResourceGroup(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> getByRoleAssignmentId(String id);

  long deleteByRoleAssignmentId(String id);
}
