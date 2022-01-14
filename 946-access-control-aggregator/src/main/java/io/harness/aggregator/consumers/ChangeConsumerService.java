package io.harness.aggregator.consumers;

import io.harness.accesscontrol.acl.persistence.ACL;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(HarnessTeam.PL)
public interface ChangeConsumerService {
  List<ACL> getAClsForRoleAssignment(RoleAssignmentDBO roleAssignmentDBO);
}
