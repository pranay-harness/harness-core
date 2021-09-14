/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroupclient;

import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResourceGroupResponse {
  @NotNull private ResourceGroupDTO resourceGroup;
  private Long createdAt;
  private Long lastModifiedAt;
  private boolean harnessManaged;

  @Builder
  public ResourceGroupResponse(
      ResourceGroupDTO resourceGroup, Long createdAt, Long lastModifiedAt, boolean harnessManaged) {
    this.resourceGroup = resourceGroup;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
    this.harnessManaged = harnessManaged;
  }
}
