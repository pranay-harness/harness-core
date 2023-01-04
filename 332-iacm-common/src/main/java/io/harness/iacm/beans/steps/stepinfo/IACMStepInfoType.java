/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.iacm.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IACMStepInfoType {
  IACM_TERRAFORM_PLAN("IACMTerraformPlan");

  private final String displayName;

  IACMStepInfoType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }
}
