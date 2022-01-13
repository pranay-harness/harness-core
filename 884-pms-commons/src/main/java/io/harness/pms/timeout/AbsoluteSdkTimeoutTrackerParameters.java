/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.timeout;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.YamlException;
import io.harness.pms.yaml.ParameterField;
import io.harness.timeout.TimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.yaml.core.timeout.Timeout;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@RecasterAlias("io.harness.pms.timeout.AbsoluteSdkTimeoutTrackerParameters")
public class AbsoluteSdkTimeoutTrackerParameters implements SdkTimeoutTrackerParameters {
  ParameterField<String> timeout;

  @Override
  public TimeoutParameters prepareTimeoutParameters() {
    if (ParameterField.isNull(timeout)) {
      throw new YamlException("Timeout field has invalid value");
    }
    if (timeout.isExpression()) {
      // Expression should be resolved before coming here
      throw new YamlException(
          String.format("Timeout field has unresolved expressions: %s", timeout.getExpressionValue()));
    }

    Timeout timeoutObj = Timeout.fromString(timeout.getValue());
    if (timeoutObj == null) {
      throw new YamlException("Timeout field has invalid value");
    }
    return AbsoluteTimeoutParameters.builder().timeoutMillis(timeoutObj.getTimeoutInMillis()).build();
  }
}
