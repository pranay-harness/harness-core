package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC) public enum EventType { TEST, PIPELINE_START, PIPELINE_COMPLETE }
