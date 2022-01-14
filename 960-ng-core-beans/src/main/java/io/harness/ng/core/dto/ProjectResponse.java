package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.ProjectConstants;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@Schema(description = "This has Project details along with its metadata as defined in Harness .")
public class ProjectResponse {
  @NotNull private ProjectDTO project;
  @Schema(description = ProjectConstants.CREATED_AT) private Long createdAt;
  @Schema(description = ProjectConstants.LAST_MODIFIED_AT) private Long lastModifiedAt;

  @Builder
  public ProjectResponse(ProjectDTO project, Long createdAt, Long lastModifiedAt) {
    this.project = project;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
  }
}