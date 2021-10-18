package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@Schema(description = "Project resource along with metadata. Generally, Used to power UI.")
public class ProjectResponse {
  @NotNull private ProjectDTO project;
  private Long createdAt;
  private Long lastModifiedAt;

  @Builder
  public ProjectResponse(ProjectDTO project, Long createdAt, Long lastModifiedAt) {
    this.project = project;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
  }
}