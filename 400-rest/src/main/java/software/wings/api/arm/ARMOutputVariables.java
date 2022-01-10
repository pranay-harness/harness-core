/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.arm;

import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;

@JsonTypeName("armOutputVariables")
public class ARMOutputVariables extends HashMap<String, Object> implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "arm";

  @Override
  public String getType() {
    return "armOutputVariables";
  }
}
