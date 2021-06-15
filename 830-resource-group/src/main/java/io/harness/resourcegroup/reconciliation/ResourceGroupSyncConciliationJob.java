package io.harness.resourcegroup.reconciliation;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.ACCOUNT;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.ORGANIZATION;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.PROJECT;

import io.harness.beans.Scope;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.exception.UnexpectedException;
import io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourceInfo;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;
import io.harness.resourcegroup.model.StaticResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector.StaticResourceSelectorKeys;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ResourceGroupSyncConciliationJob implements Runnable {
  private static final int WAIT_TIME_IN_SECONDS = 30;

  Consumer redisConsumer;
  Map<String, Resource> resourceMap;
  ResourceGroupService resourceGroupService;
  String serviceId;

  public ResourceGroupSyncConciliationJob(Consumer redisConsumer, Map<String, Resource> resourceMap,
      ResourceGroupService resourceGroupService, String serviceId) {
    this.redisConsumer = redisConsumer;
    this.resourceGroupService = resourceGroupService;
    this.serviceId = serviceId;
    this.resourceMap =
        resourceMap.values()
            .stream()
            .filter(resource -> resource.getEventFrameworkEntityType().isPresent())
            .collect(Collectors.toMap(resource -> resource.getEventFrameworkEntityType().get(), Function.identity()));
  }

  @Override
  public void run() {
    log.info("ResourceGroupSyncConciliationJob started");
    SecurityContextBuilder.setContext(new ServicePrincipal(serviceId));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        readEventsFrameworkMessages();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } finally {
      SecurityContextBuilder.unsetCompleteContext();
    }
  }

  private void readEventsFrameworkMessages() throws InterruptedException {
    try {
      pollAndProcessMessages();
    } catch (EventsFrameworkDownException e) {
      log.error("Events framework is down. Retrying", e);
      TimeUnit.SECONDS.sleep(WAIT_TIME_IN_SECONDS);
    }
  }

  private void pollAndProcessMessages() {
    List<Message> messages;
    String messageId;
    messages = redisConsumer.read(Duration.ofSeconds(WAIT_TIME_IN_SECONDS));
    for (Message message : messages) {
      messageId = message.getId();
      handleMessage(message);
      redisConsumer.acknowledge(messageId);
    }
  }

  private void handleMessage(Message message) {
    try {
      processMessage(message);
    } catch (Exception ex) {
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
    }
  }

  private void processMessage(Message message) {
    if (!message.hasMessage()) {
      return;
    }
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap == null || !metadataMap.containsKey(ACTION) || !metadataMap.containsKey(ENTITY_TYPE)) {
      return;
    }

    String entityType = metadataMap.get(ENTITY_TYPE);
    if (!resourceMap.containsKey(entityType)) {
      return;
    }
    ResourceInfo resourceInfo = resourceMap.get(entityType).getResourceInfoFromEvent(message);
    if (Objects.isNull(resourceInfo)) {
      return;
    }
    String action = metadataMap.get(ACTION);
    processMessage(resourceInfo, action);
  }

  private void processMessage(ResourceInfo resourceInfo, String action) {
    switch (action) {
      case DELETE_ACTION:
        handleDeleteEvent(resourceInfo);
        break;
      case CREATE_ACTION:
        handleCreateEvent(resourceInfo);
        break;
      default:
        break;
    }
  }

  private boolean handleCreateEvent(ResourceInfo resourceInfo) {
    String resourceType = resourceInfo.getResourceType();
    if (resourceType.equals(PROJECT) || resourceType.equals(ORGANIZATION) || resourceType.equals(ACCOUNT)) {
      resourceGroupService.createManagedResourceGroup(Scope.of(
          resourceInfo.getAccountIdentifier(), resourceInfo.getOrgIdentifier(), resourceInfo.getProjectIdentifier()));
    }
    return true;
  }

  private void handleDeleteEvent(ResourceInfo resource) {
    if (isScope(resource.getResourceType())) {
      return;
    }

    int counter = 0;
    int maxLimit = 50;
    while (counter < maxLimit) {
      Page<ResourceGroup> resourceGroupsPage =
          resourceGroupService.list(getResourceGroupsWithResource(resource), PageRequest.of(counter, 20));
      if (!resourceGroupsPage.hasContent()) {
        break;
      }
      for (ResourceGroup resourceGroup : resourceGroupsPage.getContent()) {
        deleteResourceFromResourceGroup(resource, resourceGroup);
        resourceGroupService.update(ResourceGroupMapper.toDTO(resourceGroup), false);
      }
      counter++;
    }
  }

  private boolean isScope(String resourceType) {
    return resourceType.equals(ACCOUNT) || resourceType.equals(ORGANIZATION) || resourceType.equals(PROJECT);
  }

  private Criteria getResourceGroupsWithResource(ResourceInfo resourceInfo) {
    Criteria criteria = Criteria.where(ResourceGroupKeys.accountIdentifier)
                            .is(resourceInfo.getAccountIdentifier())
                            .and(ResourceGroupKeys.orgIdentifier)
                            .is(resourceInfo.getOrgIdentifier())
                            .and(ResourceGroupKeys.projectIdentifier)
                            .is(resourceInfo.getProjectIdentifier());

    criteria.and(ResourceGroupKeys.resourceSelectors)
        .elemMatch(Criteria.where(StaticResourceSelectorKeys.resourceType)
                       .is(resourceInfo.getResourceType())
                       .and(StaticResourceSelectorKeys.identifiers)
                       .is(resourceInfo.getResourceIdentifier()));
    return criteria;
  }

  private void deleteResourceFromResourceGroup(ResourceInfo resource, ResourceGroup resourceGroup) {
    List<StaticResourceSelector> resourceSelectors =
        resourceGroup.getResourceSelectors()
            .stream()
            .filter(StaticResourceSelector.class ::isInstance)
            .map(StaticResourceSelector.class ::cast)
            .filter(rs -> rs.getResourceType().equals(resource.getResourceType()))
            .collect(Collectors.toList());

    resourceSelectors.forEach(rs -> rs.getIdentifiers().remove(resource.getResourceIdentifier()));
  }
}
