package io.harness.ng.core.dto;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrganizationResponse {
  @NotNull private OrganizationDTO organization;
  private Long createdAt;
  private Long lastModifiedAt;
  private boolean harnessManaged;

  @Builder
  public OrganizationResponse(
      OrganizationDTO organization, Long createdAt, Long lastModifiedAt, boolean harnessManaged) {
    this.organization = organization;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
    this.harnessManaged = harnessManaged;
  }
}