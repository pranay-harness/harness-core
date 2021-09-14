/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.shellscript;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
@RecasterAlias("io.harness.steps.shellscript.K8sInfraDelegateConfigOutput")
public class K8sInfraDelegateConfigOutput implements ExecutionSweepingOutput {
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
}
