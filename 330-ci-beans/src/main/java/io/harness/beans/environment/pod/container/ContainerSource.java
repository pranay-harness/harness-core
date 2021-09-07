package io.harness.beans.environment.pod.container;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CI) enum ContainerSource { BUILD_JOB, HARNESS_WORKER }
