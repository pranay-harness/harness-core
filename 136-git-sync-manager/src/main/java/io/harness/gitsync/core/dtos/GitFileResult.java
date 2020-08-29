package io.harness.gitsync.core.dtos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitFileResult {
  String filePath;
  String rootFolder;
  Status status;

  enum Status { SUCCESS, FAILED, SKIPPED }
}
