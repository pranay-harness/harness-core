package io.harness.lock;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL) public enum DistributedLockImplementation { NOOP, MONGO, REDIS }
