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
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
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
import org.apache.commons.lang3.StringUtils;

@OwnedBy(DEL)
@Slf4j
@Singleton
public class OrganizationEntityCRUDEventListener implements MessageListener {
  private final DelegateProfileServiceImpl delegateProfileService;

  @Inject
  public OrganizationEntityCRUDEventListener(final DelegateProfileServiceImpl delegateProfileService) {
    this.delegateProfileService = delegateProfileService;
  }

  @Override
  public boolean handleMessage(@NonNull final Message message) {
    if (message.hasMessage()) {
      final Map<String, String> metadata = message.getMessage().getMetadataMap();
      final String entityType = metadata.get(ENTITY_TYPE);
      if (Objects.equals(entityType, ORGANIZATION_ENTITY)) {
        try {
          final OrganizationEntityChangeDTO organizationEntityChangeDTO =
              OrganizationEntityChangeDTO.parseFrom(message.getMessage().getData());
          final String action = metadata.get(ACTION);
          if (action != null) {
            return processEntityChangeEvent(organizationEntityChangeDTO, action);
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

  private boolean processEntityChangeEvent(
      final OrganizationEntityChangeDTO organizationEntityChangeDTO, final String action) {
    if (CREATE_ACTION.equals(action)) {
      return handleCreateEvent(organizationEntityChangeDTO);
    }
    return true;
  }

  private boolean handleCreateEvent(final OrganizationEntityChangeDTO organizationEntityChangeDTO) {
    try {
      final DelegateEntityOwner owner =
          DelegateEntityOwnerHelper.buildOwner(organizationEntityChangeDTO.getIdentifier(), StringUtils.EMPTY);
      log.info("New organization created {}. Creating delegate primary config.", owner.getIdentifier());
      // We need to create only if it doesn't already exist as message can be repeated
      delegateProfileService.fetchNgPrimaryProfile(organizationEntityChangeDTO.getAccountIdentifier(), owner);
      return true;
    } catch (final Exception e) {
      log.error("Failed to create primary delegate profile for organization {}/{}",
          organizationEntityChangeDTO.getAccountIdentifier(), organizationEntityChangeDTO.getIdentifier(), e);
      return false;
    }
  }
}
