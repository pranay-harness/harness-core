package io.harness.aggregator.consumers;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.aggregator.services.apis.AggregatorService;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class RoleChangeConsumer implements ChangeConsumer {
  private final AggregatorService aggregatorService;

  @Override
  public void consumeUpdateEvent(String id, AccessControlEntity persistentEntity) {
    List<ACL> aclList = aggregatorService.processRoleUpdation((RoleDBO) persistentEntity);
    log.info("Processed role updation event, number of new ACLs created: {}", aclList.size());
  }

  @Override
  public void consumeDeleteEvent(String id) {
    log.info("Role deleted with id: {}, skipping processing it", id);
  }

  @Override
  public void consumeCreateEvent(String id, AccessControlEntity accessControlEntity) {
    log.info("New Role created with id: {}, skipping processing it", id);
  }
}
