package io.harness.pms.preflight;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE) public enum PreFlightStatus { SUCCESS, FAILURE, IN_PROGRESS, UNKNOWN }
