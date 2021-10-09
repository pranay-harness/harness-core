package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC) public enum EventStatus { QUEUED, FAILED, SUCCESS, SKIPPED }
