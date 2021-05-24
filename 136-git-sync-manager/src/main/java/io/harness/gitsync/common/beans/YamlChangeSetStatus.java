package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX) public enum YamlChangeSetStatus { QUEUED, RUNNING, FAILED, COMPLETED, SKIPPED }
