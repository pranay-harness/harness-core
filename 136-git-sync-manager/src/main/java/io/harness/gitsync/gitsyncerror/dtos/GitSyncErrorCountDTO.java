package io.harness.gitsync.gitsyncerror.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GitSyncErrorCountDTOKeys")
@Schema(name = "GitSyncErrorCount", description = "This is the total number of Git sync and connectivity errors")
@OwnedBy(PL)
public class GitSyncErrorCountDTO {
  @Schema(description = "This is the number of Git to Harness errors") long gitToHarnessErrorCount;
  @Schema(description = "This is the number of connectivity errors") long connectivityErrorCount;
}
