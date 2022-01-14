package io.harness.gitsync.gitsyncerror.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "GitSyncErrorDetails", description = "This contains Git Sync error details specific to Error Type")
@OwnedBy(PL)
public interface GitSyncErrorDetailsDTO {}
