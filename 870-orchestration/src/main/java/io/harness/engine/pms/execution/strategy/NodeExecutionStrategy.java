/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.execution.strategy;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PmsNodeExecution;
import io.harness.execution.PmsNodeExecutionMetadata;
import io.harness.plan.Node;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;

import com.google.protobuf.ByteString;
import java.util.EnumSet;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface NodeExecutionStrategy<T extends Node, N extends PmsNodeExecution, M extends PmsNodeExecutionMetadata> {
  N triggerNode(Ambiance ambiance, T node, M metadata);

  default void startExecution(Ambiance ambiance) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  }

  default void processFacilitationResponse(Ambiance ambiance, FacilitatorResponseProto facilitatorResponseProto) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  }

  default void processStartEventResponse(Ambiance ambiance, ExecutableResponse executableResponse) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  }

  default void resumeNodeExecution(Ambiance ambiance, Map<String, ByteString> response, boolean asyncError) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  }

  default void processStepResponse(Ambiance ambiance, StepResponseProto stepResponse) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  }

  default void concludeExecution(Ambiance ambiance, Status status, EnumSet<Status> overrideStatus) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  }

  default void processAdviserResponse(Ambiance ambiance, AdviserResponse adviserResponse) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  }

  default void endNodeExecution(Ambiance ambiance) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  }

  void handleError(Ambiance ambiance, Exception exception);
}
