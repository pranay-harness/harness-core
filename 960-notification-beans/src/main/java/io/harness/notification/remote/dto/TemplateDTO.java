/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
