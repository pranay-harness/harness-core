/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.executioncapability;

import static java.util.Collections.singletonList;

import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import lombok.Value;

@Value
public class ConnectivityCapabilityDemander implements ExecutionCapabilityDemander {
  String host;
  int port;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return singletonList(
        SocketConnectivityExecutionCapability.builder().hostName(host).port(String.valueOf(port)).build());
  }
}
