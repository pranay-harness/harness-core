/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.remote.dto;

import io.harness.Team;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplateDTO {
  private String identifier;
  private Team team;
  private long createdAt;
  private long lastModifiedAt;
  private byte[] file;
}
