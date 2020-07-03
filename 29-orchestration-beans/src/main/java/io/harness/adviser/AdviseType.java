package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC) public enum AdviseType { NEXT_STEP, RETRY, END_PLAN }
