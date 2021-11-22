package io.harness.gitsync.common.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.FullSyncEventRequest;
import io.harness.gitsync.common.dtos.TriggerFullSyncRequestDTO;
import io.harness.gitsync.common.dtos.TriggerFullSyncResponseDTO;
import io.harness.gitsync.common.service.FullSyncTriggerService;
import io.harness.gitsync.core.fullsync.FullSyncAccumulatorService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class FullSyncTriggerServiceImpl implements FullSyncTriggerService {
  FullSyncAccumulatorService fullSyncAccumulatorService;
  Producer eventProducer;

  @Inject
  public FullSyncTriggerServiceImpl(FullSyncAccumulatorService fullSyncAccumulatorService,
      @Named(EventsFrameworkConstants.GIT_FULL_SYNC_STREAM) Producer fullSyncEventProducer) {
    this.fullSyncAccumulatorService = fullSyncAccumulatorService;
    this.eventProducer = fullSyncEventProducer;
  }

  @Override
  public TriggerFullSyncResponseDTO triggerFullSync(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, TriggerFullSyncRequestDTO fullSyncRequest) {
    final String messageId = sendEventForFullSync(accountIdentifier, orgIdentifier, projectIdentifier, fullSyncRequest);
    if (messageId == null) {
      return TriggerFullSyncResponseDTO.builder().isFullSyncTriggered(false).build();
    }
    log.info("Triggered full sync with the message id {}", messageId);
    return TriggerFullSyncResponseDTO.builder().isFullSyncTriggered(true).build();
  }

  private String sendEventForFullSync(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      TriggerFullSyncRequestDTO fullSyncRequest) {
    final EntityScopeInfo.Builder entityScopeInfoBuilder =
        EntityScopeInfo.newBuilder()
            .setAccountId(accountIdentifier)
            .setIdentifier(fullSyncRequest.getYamlGitConfigIdentifier());
    if (isNotEmpty(orgIdentifier)) {
      entityScopeInfoBuilder.setOrgId(StringValue.of(orgIdentifier));
    }
    if (isNotEmpty(projectIdentifier)) {
      entityScopeInfoBuilder.setProjectId(StringValue.of(projectIdentifier));
    }

    final FullSyncEventRequest.Builder builder = FullSyncEventRequest.newBuilder()
                                                     .setGitConfigScope(entityScopeInfoBuilder.build())
                                                     .setBranch(fullSyncRequest.getBranch())
                                                     .setCreatePr(fullSyncRequest.isCreatePR());

    if (fullSyncRequest.isCreatePR()) {
      builder.setTargetBranch(fullSyncRequest.getTargetBranchForPR());
    }

    try {
      final String messageId = eventProducer.send(Message.newBuilder()
                                                      .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier))
                                                      .setData(builder.build().toByteString())
                                                      .build());
      log.info("Produced event with id [{}] for full sync for accountId [{}]  for yamlgitconfig [{}]", messageId,
          accountIdentifier, fullSyncRequest.getYamlGitConfigIdentifier());
      return messageId;
    } catch (Exception e) {
      log.error("Event to send git config update failed for accountId [{}] for yamlgitconfig [{}]", accountIdentifier,
          fullSyncRequest.getYamlGitConfigIdentifier(), e);
      return null;
    }
  }
}
