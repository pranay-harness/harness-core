package io.harness.annotations;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CE) public enum ChangeDataCaptureSink { TIMESCALE, BQ }
