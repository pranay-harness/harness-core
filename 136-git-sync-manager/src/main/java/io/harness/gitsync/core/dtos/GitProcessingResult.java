package io.harness.gitsync.core.dtos;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitProcessingResult {
  List<GitFileResult> gitFileResult;
  String commitId;
  String connectorId;
  String branchName;
}
