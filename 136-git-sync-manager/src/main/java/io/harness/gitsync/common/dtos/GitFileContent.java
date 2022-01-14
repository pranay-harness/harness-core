package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Schema(name = "GitFileContent", description = "This contains content of Git File")
@OwnedBy(DX)
public class GitFileContent {
  @Schema(description = "Git File Content") String content;
  @Schema(description = "Object Id of the Git File") String objectId;
}
