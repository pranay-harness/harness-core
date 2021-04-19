package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.GitToHarnessProcessRequest;
import io.harness.gitsync.ProcessingResponse;

@OwnedBy(DX)
public interface GitToHarnessProcessor {
  ProcessingResponse gitToHarnessProcessingRequest(GitToHarnessProcessRequest changeSets);
}
