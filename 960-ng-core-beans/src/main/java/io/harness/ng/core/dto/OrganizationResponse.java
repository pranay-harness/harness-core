package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.OrganizationConstants;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@Schema(description = "This has details of the Organization along with its metadata in Harness.")
public class OrganizationResponse {
  @NotNull private OrganizationDTO organization;
  @Schema(description = OrganizationConstants.CREATED_AT) private Long createdAt;
  @Schema(description = OrganizationConstants.LAST_MODIFIED_AT) private Long lastModifiedAt;
  @Schema(description = OrganizationConstants.HARNESS_MANAGED) private boolean harnessManaged;

  @Builder
  public OrganizationResponse(
      OrganizationDTO organization, Long createdAt, Long lastModifiedAt, boolean harnessManaged) {
    this.organization = organization;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
    this.harnessManaged = harnessManaged;
  }
}