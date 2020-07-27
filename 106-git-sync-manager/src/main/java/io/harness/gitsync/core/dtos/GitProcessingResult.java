package io.harness.gitsync.core.dtos;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GitProcessingResult {
  List<GitFileResult> gitFileResult;
  String commitId;
  String connectorId;
  String branchName;
}
