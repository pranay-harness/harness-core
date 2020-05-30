package io.harness.facilitator.modes;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC) @Redesign public enum ExecutionMode { SYNC, ASYNC, SKIP, TASK_CHAIN, CHILDREN, CHILD, ASYNC_TASK }
