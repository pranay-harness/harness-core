package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.GitToHarnessProcessRequest;
import io.harness.gitsync.MarkEntityInvalidRequest;
import io.harness.gitsync.MarkEntityInvalidResponse;
import io.harness.gitsync.ProcessingResponse;

@OwnedBy(DX)
public interface GitToHarnessSdkProcessor {
  ProcessingResponse gitToHarnessProcessingRequest(GitToHarnessProcessRequest changeSets);

  MarkEntityInvalidResponse markEntitiesInvalid(MarkEntityInvalidRequest markEntityInvalidRequest);
}
