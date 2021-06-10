package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponseDTO;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepType;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitToHarnessProgressDTO {
  @NotNull private String uuid;
  @NotNull private String accountIdentifier;
  @NotNull private String yamlChangeSetId;
  @NotNull private String repoUrl;
  @NotNull private String branch;
  @NotNull private YamlChangeSetEventType eventType;
  @NotNull private GitToHarnessProcessingStepType stepType;
  @NotNull private GitToHarnessProcessingStepStatus stepStatus;
  @NotNull private Long stepStartingTime;
  List<GitToHarnessFileProcessingRequest> gitFileChanges;
  List<GitToHarnessProcessingResponseDTO> processingResponse;
}
