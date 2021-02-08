package io.harness.resourcegroup.remote.dto;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResourceGroupResponse {
  @NotNull private ResourceGroupDTO resourceGroupDTO;
  private Long createdAt;
  private Long lastModifiedAt;

  @Builder
  public ResourceGroupResponse(ResourceGroupDTO resourceGroupDTO, Long createdAt, Long lastModifiedAt) {
    this.resourceGroupDTO = resourceGroupDTO;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
  }
}