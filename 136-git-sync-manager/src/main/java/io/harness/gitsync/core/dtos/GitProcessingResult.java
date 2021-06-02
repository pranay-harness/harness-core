package io.harness.gitsync.core.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitProcessingResult {
  List<GitFileResult> gitFileResult;
  String commitId;
  String connectorId;
  String branchName;
}
