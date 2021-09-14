/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.remote.mappers;

import io.harness.notification.entities.NotificationTemplate;
import io.harness.notification.remote.dto.TemplateDTO;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TemplateMapper {
  public static Optional<TemplateDTO> toDTO(NotificationTemplate template) {
    if (!Optional.ofNullable(template).isPresent()) {
      return Optional.empty();
    }
    return Optional.of(TemplateDTO.builder()
                           .team(template.getTeam())
                           .identifier(template.getIdentifier())
                           .createdAt(template.getCreatedAt())
                           .lastModifiedAt(template.getLastUpdatedAt())
                           .file(template.getFile())
                           .build());
  }
}
