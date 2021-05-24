package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX) public enum YamlChangeSetEventType { GIT_TO_HARNESS_PUSH, BRANCH_SYNC, BRANCH_CREATE }
