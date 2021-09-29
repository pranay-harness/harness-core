package io.harness.engine.pms.execution.strategy;

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

public interface NodeExecutionStrategy<T extends Node> {
  void triggerNode(Ambiance ambiance, T node);

  default void startExecution(Ambiance ambiance) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  };

  default void processFacilitationResponse(Ambiance ambiance, FacilitatorResponseProto facilitatorResponseProto) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  };

  default void processStartEventResponse(Ambiance ambiance, ExecutableResponse executableResponse) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  };

  default void resumeNodeExecution(Ambiance ambiance, Map<String, ByteString> response, boolean asyncError) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  };

  default void processStepResponse(Ambiance ambiance, StepResponseProto stepResponse) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  };

  default void concludeExecution(Ambiance ambiance, Status status, EnumSet<Status> overrideStatus) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  };

  default void processAdviserResponse(Ambiance ambiance, AdviserResponse adviserResponse) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  };

  default void endNodeExecution(Ambiance ambiance) {
    throw new UnsupportedOperationException("Start execution node Supported for plan");
  };

  void handleError(Ambiance ambiance, Exception exception);
}
