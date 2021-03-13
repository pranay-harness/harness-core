package io.harness.aggregator.consumers;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.aggregator.services.apis.AggregatorService;

import com.google.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class RoleAssignmentChangeConsumer implements ChangeConsumer {
  private final AggregatorService aggregatorService;

  @Override
  public void consumeUpdateEvent(String id, AccessControlEntity persistentEntity) {
    List<ACL> aclList = aggregatorService.processRoleAssignmentUpdation((RoleAssignmentDBO) persistentEntity);
    log.info("Number of new ACLs created: {}", aclList.size());
  }

  @Override
  public void consumeDeleteEvent(String id) {
    aggregatorService.processRoleAssignmentDeletion(id);
  }

  @Override
  public void consumeCreateEvent(String id, AccessControlEntity accessControlEntity) {
    aggregatorService.processRoleAssignmentCreation((RoleAssignmentDBO) accessControlEntity);
  }
}
