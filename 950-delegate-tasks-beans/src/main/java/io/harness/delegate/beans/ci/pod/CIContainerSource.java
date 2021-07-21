package io.harness.delegate.beans.ci.pod;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CI) enum CIContainerSource { BUILD_JOB, HARNESS_WORKER }
