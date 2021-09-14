/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder()
@JsonTypeName("cloudFormationCompletionFlag")
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class CloudFormationCompletionFlag implements SweepingOutput {
  private boolean createStackCompleted;

  @Override
  public String getType() {
    return "cloudFormationCompletionFlag";
  }
}
