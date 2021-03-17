package io.harness.resourcegroup.reconciliation;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.RESTORE_ACTION;
import static io.harness.resourcegroup.framework.beans.ResourceGroupConstants.ENTITY_CRUD;

import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourcePrimaryKey;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SyncConciliationJob implements Runnable {
  Consumer redisConsumer;
  Map<String, ResourceValidator> resourceValidators;
  ResourceGroupService resourceGroupService;
  String serviceId;
  Set<String> validResourceTypes;

  @Inject
  public SyncConciliationJob(@Named(ENTITY_CRUD) Consumer redisConsumer,
      @Named("resourceValidatorMap") Map<String, ResourceValidator> resourceValidators,
      ResourceGroupService resourceGroupService, @Named("serviceId") String serviceId) {
    this.redisConsumer = redisConsumer;
    this.resourceGroupService = resourceGroupService;
    this.serviceId = serviceId;
    this.resourceValidators =
        resourceValidators.values()
            .stream()
            .filter(e -> e.getEventFrameworkEntityType().isPresent())
            .collect(Collectors.toMap(e -> e.getEventFrameworkEntityType().get(), Function.identity()));
    this.validResourceTypes = resourceValidators.values()
                                  .stream()
                                  .map(ResourceValidator::getEventFrameworkEntityType)
                                  .filter(Optional::isPresent)
                                  .map(Optional::get)
                                  .collect(Collectors.toSet());
  }

  @Override
  public void run() {
    log.info("Started the consumer for entity crud stream");
    SecurityContextBuilder.setContext(new ServicePrincipal(serviceId));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        pollAndProcessMessages();
      }
    } catch (Exception ex) {
      log.error("Entity crud stream consumer unexpectedly stopped", ex);
    }
    SecurityContextBuilder.unsetCompleteContext();
  }

  private void pollAndProcessMessages() throws ConsumerShutdownException {
    List<Message> messages;
    String messageId;
    boolean messageProcessed;
    messages = redisConsumer.read(Duration.ofSeconds(10));
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
    if (!validResourceTypes.contains(entityType)) {
      return success;
    }
    ResourcePrimaryKey resourcePrimaryKey = resourceValidators.get(entityType).getResourceGroupKeyFromEvent(message);
    if (Objects.isNull(resourcePrimaryKey)) {
      return success;
    }
    String action = metadataMap.get(ACTION);
    success = processMessage(resourcePrimaryKey, action);
    return success;
  }

  private boolean processMessage(ResourcePrimaryKey resourcePrimaryKey, String action) {
    boolean success = true;
    if (action.equals(DELETE_ACTION)) {
      success = resourceGroupService.handleResourceDeleteEvent(resourcePrimaryKey);
    } else {
      if (action.equals(CREATE_ACTION)) {
        success = resourceGroupService.createDefaultResourceGroup(resourcePrimaryKey);
      } else if (action.equals(RESTORE_ACTION)) {
        success = resourceGroupService.restoreResourceGroupsUnderHierarchy(resourcePrimaryKey);
      }
    }
    return success;
  }
}
