/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.event.listener;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PROJECT_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;

import software.wings.service.impl.DelegateProfileServiceImpl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(DEL)
@Slf4j
@Singleton
public class ProjectEntityCRUDEventListener implements MessageListener {
  private final DelegateProfileServiceImpl delegateProfileService;

  @Inject
  public ProjectEntityCRUDEventListener(final DelegateProfileServiceImpl delegateProfileService) {
    this.delegateProfileService = delegateProfileService;
  }

  @Override
  public boolean handleMessage(@NonNull final Message message) {
    if (message.hasMessage()) {
      final Map<String, String> metadata = message.getMessage().getMetadataMap();
      final String entityType = metadata.get(ENTITY_TYPE);
      if (Objects.equals(entityType, PROJECT_ENTITY)) {
        try {
          final ProjectEntityChangeDTO projectEntityChangeDTO =
              ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
          final String action = metadata.get(ACTION);
          if (action != null) {
            return processEntityChangeEvent(projectEntityChangeDTO, action);
          }
        } catch (final InvalidProtocolBufferException e) {
          throw new InvalidRequestException(
              String.format("Exception in unpacking EntityChangeDTO for key %s", message.getId()), e);
        }
      }
    }
    // Not great, but since filtering happens on the listener level, we need to acknowledge if the message is not for us
    return true;
  }

  private boolean processEntityChangeEvent(final ProjectEntityChangeDTO projectEntityChangeDTO, final String action) {
    if (CREATE_ACTION.equals(action)) {
      return handleCreateEvent(projectEntityChangeDTO);
    }
    return true;
  }

  private boolean handleCreateEvent(final ProjectEntityChangeDTO projectEntityChangeDTO) {
    try {
      final DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(
          projectEntityChangeDTO.getOrgIdentifier(), projectEntityChangeDTO.getIdentifier());
      log.info("New project created {}. Creating delegate primary config.", owner.getIdentifier());
      // We need to create only if it doesn't already exist as message can be repeated
      delegateProfileService.fetchNgPrimaryProfile(projectEntityChangeDTO.getAccountIdentifier(), owner);
      return true;
    } catch (final Exception e) {
      log.error("Failed to create primary delegate profile for project {}/{}/{}",
          projectEntityChangeDTO.getAccountIdentifier(), projectEntityChangeDTO.getOrgIdentifier(),
          projectEntityChangeDTO.getIdentifier(), e);
      return false;
    }
  }
}
