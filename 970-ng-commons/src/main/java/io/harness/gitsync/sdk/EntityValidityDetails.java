package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "EntityGitDetails", description = "This contains Validity Details of the Entity")
@OwnedBy(PL)
public class EntityValidityDetails {
  @Schema(description = "Indicates if the Entity is valid") private boolean valid;
  @Schema(description = "This has the Git File content if the entity is invalid") private String invalidYaml;
}
