package io.harness.gitsync.gittoharness;

import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.ProcessingResponse;

public interface GitToHarnessProcessor {
  ProcessingResponse process(ChangeSets changeSets);
}
