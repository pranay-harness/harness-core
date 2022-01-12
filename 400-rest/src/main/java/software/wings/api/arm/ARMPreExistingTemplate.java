/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.arm;

import io.harness.beans.SweepingOutput;
import io.harness.delegate.task.azure.arm.AzureARMPreDeploymentData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("armPreExistingTemplate")
public class ARMPreExistingTemplate implements SweepingOutput {
  private final AzureARMPreDeploymentData preDeploymentData;

  @Override
  public String getType() {
    return "armPreExistingTemplate";
  }
}
