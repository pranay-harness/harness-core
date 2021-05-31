package io.harness.gitsync.common.beans;

import io.harness.gitsync.ChangeType;
import io.harness.product.ci.scm.proto.FileContent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitToHarnessFileProcessingRequest {
  FileContent fileDetails;
  ChangeType changeType;
}
