package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC) public enum TimeoutTrackerState { TICKING, PAUSED, EXPIRED }
