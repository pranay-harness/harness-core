/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SocketConnectivityExecutionCapability implements ExecutionCapability {
  private final CapabilityType capabilityType = CapabilityType.SOCKET;

  protected String hostName;
  protected String scheme;
  protected String port;
  protected String url;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    // maintaining backward compatibility for now
    if (shouldUseOriginalUrl()) {
      return url;
    }

    StringBuilder builder = new StringBuilder(128);
    if (isNotBlank(scheme)) {
      builder.append(scheme).append("://");
    }

    builder.append(hostName);

    if (isNotBlank(port)) {
      builder.append(':').append(port);
    }
    return builder.toString();
  }

  private boolean shouldUseOriginalUrl() {
    return isBlank(scheme) || isBlank(hostName);
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
