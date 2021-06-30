package io.harness.aggregator.consumers;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;

import java.util.List;

public interface ChangeConsumerService {
  List<ACL> getAClsForRoleAssignment(RoleAssignmentDBO roleAssignmentDBO);
}
