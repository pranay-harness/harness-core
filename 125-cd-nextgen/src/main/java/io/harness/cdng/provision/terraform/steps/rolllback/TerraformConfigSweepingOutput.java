/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.provision.terraform.steps.rolllback;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.provision.terraform.TerraformConfig;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("TerraformConfigSweepingOutput")
@JsonTypeName("TerraformConfigSweepingOutput")
@RecasterAlias("io.harness.cdng.provision.terraform.steps.rolllback.TerraformConfigSweepingOutput")
public class TerraformConfigSweepingOutput implements ExecutionSweepingOutput {
  TerraformConfig terraformConfig;
  TFTaskType tfTaskType;
}
