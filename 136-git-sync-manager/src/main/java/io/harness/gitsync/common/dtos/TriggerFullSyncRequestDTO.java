package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "FullSyncRequest", description = "The full sync request")
@OwnedBy(DX)
public class TriggerFullSyncRequestDTO {
  boolean createPR;
  @NotNull String branch;
  String targetBranchForPR;
  String commitMessage;
  @NotNull String yamlGitConfigIdentifier;
}
