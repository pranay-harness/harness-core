package io.harness.resourcegroup.reconciliation;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.RESTORE_ACTION;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.ACCOUNT;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.ORGANIZATION;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.PROJECT;

import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.lock.PersistentLocker;
import io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourceInfo;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;
import io.harness.resourcegroup.model.StaticResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector.StaticResourceSelectorKeys;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;
import io.harness.utils.ScopeUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ResourceGroupSyncConciliationJob implements Runnable {
  private static final int WAIT_TIME_IN_SECONDS = 10;
  static String DEFAULT_RESOURCE_GROUP_NAME = "All Resources";
  static String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
  static String DESCRIPTION_FORMAT = "All the resources in this %s are included in this resource group.";
  static String LOCK_NAME = ResourceGroupSyncConciliationJob.class.getName();
  Consumer redisConsumer;
  Map<String, Resource> resourceMap;
  ResourceGroupService resourceGroupService;
  String serviceId;

  @Inject
  public ResourceGroupSyncConciliationJob(@Named(EventsFrameworkConstants.ENTITY_CRUD) Consumer redisConsumer,
      Map<String, Resource> resourceMap, ResourceGroupService resourceGroupService, PersistentLocker persistentLocker,
      @Named("serviceId") String serviceId) {
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
    log.info("Started the consumer for resource group concliation");
    try {
      SecurityContextBuilder.setContext(new ServicePrincipal(serviceId));
      while (!Thread.currentThread().isInterrupted()) {
        readEventsFrameworkMessages();
      }
    } catch (InterruptedException ex) {
      SecurityContextBuilder.unsetCompleteContext();
      Thread.currentThread().interrupt();
    } catch (Exception ex) {
      log.error("resource group concliation consumer unexpectedly stopped", ex);
    } finally {
      SecurityContextBuilder.unsetCompleteContext();
    }
  }

  private void readEventsFrameworkMessages() throws InterruptedException {
    try {
      pollAndProcessMessages();
    } catch (EventsFrameworkDownException e) {
      log.error("Events framework is down for resource group concliation consumer. Retrying again...", e);
      TimeUnit.SECONDS.sleep(WAIT_TIME_IN_SECONDS);
    }
  }

  private void pollAndProcessMessages() {
    List<Message> messages;
    String messageId;
    boolean messageProcessed;
    messages = redisConsumer.read(Duration.ofSeconds(WAIT_TIME_IN_SECONDS));
    for (Message message : messages) {
      messageId = message.getId();
      messageProcessed = handleMessage(message);
      if (messageProcessed) {
        redisConsumer.acknowledge(messageId);
      }
    }
  }

  private boolean handleMessage(Message message) {
    try {
      return processMessage(message);
    } catch (Exception ex) {
      // This is not evicted from events framework so that it can be processed
      // by other consumer if the error is a runtime error
      log.error(String.format("Error occurred in processing message with id %s", message.getId()), ex);
      return false;
    }
  }

  private boolean processMessage(Message message) {
    boolean success = true;
    if (!message.hasMessage()) {
      return success;
    }
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap == null || !metadataMap.containsKey(ACTION) || !metadataMap.containsKey(ENTITY_TYPE)) {
      return success;
    }

    String entityType = metadataMap.get(ENTITY_TYPE);
    if (!resourceMap.containsKey(entityType)) {
      return success;
    }
    ResourceInfo resourceInfo = resourceMap.get(entityType).getResourceInfoFromEvent(message);
    if (Objects.isNull(resourceInfo)) {
      return success;
    }
    String action = metadataMap.get(ACTION);
    success = processMessage(resourceInfo, action);
    return success;
  }

  private boolean processMessage(ResourceInfo resourceInfo, String action) {
    switch (action) {
      case DELETE_ACTION:
        return handleDeleteEvent(resourceInfo);
      case CREATE_ACTION:
        return handleCreateEvent(resourceInfo);
      case RESTORE_ACTION:
        return handleRestoreEvent(resourceInfo);
      default:
        return true;
    }
  }

  private boolean handleDeleteEvent(ResourceInfo resourceInfo) {
    Criteria criteria = getCriteriaForResourceDeleteEvent(resourceInfo);
    String resourceType = resourceInfo.getResourceType();
    boolean isResourceTypeAlsoAScope = isResourceTypeAlsoAScope(resourceInfo.getResourceType());
    int counter = 0;
    int maxLimit = 50;
    while (counter < maxLimit) {
      Pageable pageable = org.springframework.data.domain.PageRequest.of(isResourceTypeAlsoAScope ? 0 : counter, 20);
      Page<ResourceGroup> resourceGroupsPage = resourceGroupService.list(criteria, pageable);
      if (!resourceGroupsPage.hasContent()) {
        break;
      }
      for (ResourceGroup resourceGroup : resourceGroupsPage.getContent()) {
        if (isResourceTypeAlsoAScope) {
          resourceGroupService.delete(resourceGroup.getIdentifier(), resourceGroup.getAccountIdentifier(),
              resourceGroup.getOrgIdentifier(), resourceGroup.getProjectIdentifier(), true);
        } else {
          deleteResourceFromGroup(resourceInfo, resourceType, resourceGroup);
          resourceGroupService.update(ResourceGroupMapper.toDTO(resourceGroup));
        }
      }
      counter++;
    }
    return true;
  }

  private boolean isResourceTypeAlsoAScope(String resourceType) {
    return resourceType.equals(ACCOUNT) || resourceType.equals(ORGANIZATION) || resourceType.equals(PROJECT);
  }

  private Criteria getCriteriaForResourceDeleteEvent(ResourceInfo resourceInfo) {
    String resourceType = resourceInfo.getResourceType();
    Criteria criteria = Criteria.where(ResourceGroupKeys.accountIdentifier)
                            .is(resourceInfo.getAccountIdentifier())
                            .and(ResourceGroupKeys.orgIdentifier)
                            .is(resourceInfo.getOrgIdentifier())
                            .and(ResourceGroupKeys.projectIdentifier)
                            .is(resourceInfo.getProjectIdentifier())
                            .and(ResourceGroupKeys.deleted)
                            .is(false);

    if (isResourceTypeAlsoAScope(resourceType)) {
      return criteria;
    }
    criteria.and(ResourceGroupKeys.resourceSelectors)
        .elemMatch(Criteria.where(StaticResourceSelectorKeys.resourceType)
                       .is(resourceInfo.getResourceType())
                       .and(StaticResourceSelectorKeys.identifiers)
                       .is(resourceInfo.getResourceIdentifier()));
    return criteria;
  }

  private void deleteResourceFromGroup(ResourceInfo resourceInfo, String resourceType, ResourceGroup resourceGroup) {
    Optional<StaticResourceSelector> resourceSelectorOpt = resourceGroup.getResourceSelectors()
                                                               .stream()
                                                               .filter(StaticResourceSelector.class ::isInstance)
                                                               .map(StaticResourceSelector.class ::cast)
                                                               .filter(rs -> rs.getResourceType().equals(resourceType))
                                                               .findFirst();
    if (!resourceSelectorOpt.isPresent()) {
      throw new IllegalStateException("Panic situation. Must have StaticResourceSelector for " + resourceType);
    }
    StaticResourceSelector resourceSelector = resourceSelectorOpt.get();
    boolean isRemoved = resourceSelector.getIdentifiers().remove(resourceInfo.getResourceIdentifier());
    if (!isRemoved) {
      throw new IllegalStateException(
          "Panic situation. Must have resourceIdentifier " + resourceInfo.getResourceIdentifier());
    }
    if (resourceSelector.getIdentifiers().isEmpty()) {
      resourceGroup.getResourceSelectors().remove(resourceSelector);
    }
  }

  private boolean handleRestoreEvent(ResourceInfo resourceInfo) {
    String resourceType = resourceInfo.getResourceType();
    if (resourceType.equals(PROJECT) || resourceType.equals(ORGANIZATION) || resourceType.equals(ACCOUNT)) {
      return resourceGroupService.restoreAll(
          resourceInfo.getAccountIdentifier(), resourceInfo.getOrgIdentifier(), resourceInfo.getProjectIdentifier());
    }
    return true;
  }

  private boolean handleCreateEvent(ResourceInfo resourceInfo) {
    String resourceType = resourceInfo.getResourceType();
    if (resourceType.equals(PROJECT) || resourceType.equals(ORGANIZATION) || resourceType.equals(ACCOUNT)) {
      ResourceGroupDTO resourceGroupDTO =
          ResourceGroupDTO.builder()
              .accountIdentifier(resourceInfo.getAccountIdentifier())
              .orgIdentifier(resourceInfo.getOrgIdentifier())
              .projectIdentifier(resourceInfo.getProjectIdentifier())
              .name(DEFAULT_RESOURCE_GROUP_NAME)
              .identifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
              .description(String.format(DESCRIPTION_FORMAT,
                  ScopeUtils
                      .getMostSignificantScope(resourceInfo.getAccountIdentifier(), resourceInfo.getOrgIdentifier(),
                          resourceInfo.getProjectIdentifier())
                      .toString()
                      .toLowerCase()))
              .resourceSelectors(Collections.emptyList())
              .fullScopeSelected(true)
              .build();
      resourceGroupService.createManagedResourceGroup(resourceInfo.getAccountIdentifier(),
          resourceInfo.getOrgIdentifier(), resourceInfo.getProjectIdentifier(), resourceGroupDTO);
    }
    return true;
  }
}
