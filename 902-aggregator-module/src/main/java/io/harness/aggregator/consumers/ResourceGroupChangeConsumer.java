package io.harness.aggregator.consumers;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.aggregator.services.apis.AggregatorService;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class ResourceGroupChangeConsumer implements ChangeConsumer<ResourceGroupDBO> {
  private final AggregatorService aggregatorService;

  @Override
  public void consumeUpdateEvent(String id, ResourceGroupDBO resourceGroupDBO) {
    List<ACL> acls = aggregatorService.processResourceGroupUpdation(resourceGroupDBO);
    log.info("Received resource group updation event for id: {}, number of new ACLs created: {}", id, acls.size());
  }

  @Override
  public void consumeDeleteEvent(String id) {
    log.info("Received resource group deletion event for id: {}, skipping processing it...", id);
  }

  @Override
  public void consumeCreateEvent(String id, ResourceGroupDBO accessControlEntity) {
    log.info("Received resource group creation event for id: {}, skipping processing it...", id);
  }
}
