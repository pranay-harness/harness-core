/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.alert;

import io.harness.alert.AlertData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuntimeInputsRequiredAlert implements AlertData {
  private String executionId;
  private String stateExecutionInstanceId;
  private String name;
  private String envId;

  @Override
  public boolean matches(AlertData alertData) {
    return executionId.equals(((RuntimeInputsRequiredAlert) alertData).getExecutionId());
  }

  @Override
  public String buildTitle() {
    return name + " requires runtime inputs";
  }
}
