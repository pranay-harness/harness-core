package io.harness.git.model;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX) public enum ChangeType { ADD, RENAME, MODIFY, DELETE, NONE }
