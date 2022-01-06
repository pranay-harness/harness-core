/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.concurrency;

import io.harness.distribution.constraint.Consumer;

import software.wings.beans.WorkflowExecution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import org.codehaus.jackson.annotate.JsonTypeName;

@Value
@Builder
@JsonTypeName("concurrentExecutionResponse")
public class ConcurrentExecutionResponse {
  Consumer.State state;
  ConcurrencyStrategy.UnitType unitType;
  List<WorkflowExecution> executions;
  @Default Map<String, Object> infrastructureDetails = new HashMap<>();
}
