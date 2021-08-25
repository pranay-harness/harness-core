package io.harness.ngtriggers.beans.entity.metadata.status;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE) public enum StatusResult { SUCCESS, FAILED, UNKNOWN }
