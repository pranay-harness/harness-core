package io.harness.cvng.core.jobs;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.alert.entities.AlertRule;
import io.harness.cvng.cd10.entities.CD10Mapping;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DeleteEntityByProjectHandler;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ProjectChangeEventMessageProcessor implements ConsumerMessageProcessor {
  @Inject private Injector injector;
  @VisibleForTesting
  static final Map<Class<? extends PersistentEntity>, Class<? extends DeleteEntityByProjectHandler>> ENTITIES_MAP;

  static {
    ENTITIES_MAP = new HashMap<>();
    ENTITIES_MAP.put(CVConfig.class, CVConfigService.class);
    final List<Class<? extends PersistentEntity>> deleteEntitiesWithDefaultHandler =
        Arrays.asList(VerificationJob.class, Activity.class, AlertRule.class, CD10Mapping.class, MetricPack.class,
            ActivitySource.class, HeatMap.class, TimeSeriesThreshold.class);
    deleteEntitiesWithDefaultHandler.forEach(entity -> ENTITIES_MAP.put(entity, DeleteEntityByProjectHandler.class));
  }
  @Inject private HPersistence hPersistence;
  @Override
  public void processMessage(Message message) {
    Preconditions.checkState(validateMessage(message), "Invalid message received by Project Change Event Processor");

    ProjectEntityChangeDTO projectEntityChangeDTO;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ProjectEntityChangeDTO for key {}", message.getId(), e);
      throw new IllegalStateException(e);
    }

    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.containsKey(EventsFrameworkConstants.ACTION_METADATA)) {
      switch (metadataMap.get(EventsFrameworkConstants.ACTION_METADATA)) {
        case EventsFrameworkConstants.CREATE_ACTION:
          processCreateAction(projectEntityChangeDTO);
          return;
        case EventsFrameworkConstants.DELETE_ACTION:
          processDeleteAction(projectEntityChangeDTO);
          return;
        default:
      }
    }
  }

  private void processCreateAction(ProjectEntityChangeDTO projectEntityChangeDTO) {}

  @VisibleForTesting
  void processDeleteAction(ProjectEntityChangeDTO projectEntityChangeDTO) {
    ENTITIES_MAP.forEach((entity, handler)
                             -> injector.getInstance(handler).deleteByProjectIdentifier(entity,
                                 projectEntityChangeDTO.getAccountIdentifier(),
                                 projectEntityChangeDTO.getOrgIdentifier(), projectEntityChangeDTO.getIdentifier()));
  }

  private boolean validateMessage(Message message) {
    return message != null && message.hasMessage() && message.getMessage().getMetadataMap() != null
        && message.getMessage().getMetadataMap().containsKey(EventsFrameworkConstants.ENTITY_TYPE_METADATA)
        && EventsFrameworkConstants.PROJECT_ENTITY.equals(
            message.getMessage().getMetadataMap().get(EventsFrameworkConstants.ENTITY_TYPE_METADATA));
  }
}
