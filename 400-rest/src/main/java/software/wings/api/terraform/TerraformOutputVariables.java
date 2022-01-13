/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;

@JsonTypeName("terraformOutputVariables")
@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDP)
public class TerraformOutputVariables extends HashMap<String, Object> implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "terraform";

  @Override
  public String getType() {
    return "terraformOutputVariables";
  }
}
