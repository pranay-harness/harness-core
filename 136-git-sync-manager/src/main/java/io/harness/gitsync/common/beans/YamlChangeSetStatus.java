package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.Arrays;
import java.util.List;

@OwnedBy(DX)
public enum YamlChangeSetStatus {
  QUEUED,
  RUNNING,
  FAILED,
  COMPLETED,
  SKIPPED,
  FAILED_WITH_RETRY;

  public static List<YamlChangeSetStatus> getTerminalStatusList() {
    return Arrays.asList(FAILED, COMPLETED, SKIPPED);
  }
}
