package io.harness.resourcegroup.remote.dto;

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

  @Builder
  public ResourceGroupResponse(ResourceGroupDTO resourceGroup, Long createdAt, Long lastModifiedAt) {
    this.resourceGroup = resourceGroup;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
  }
}