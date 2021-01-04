package io.harness.pms.sdk.core.execution;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.PlanNodeProto;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface ExecuteStrategy {
  void start(InvokerPackage invokerPackage);

  default void resume(ResumePackage resumePackage) {
    throw new UnsupportedOperationException();
  }

  default PlanNodeProto findNode(List<PlanNodeProto> nodes, String nodeId) {
    int nodeIndex = Collections.binarySearch(
        nodes, PlanNodeProto.newBuilder().setUuid(nodeId).build(), Comparator.comparing(PlanNodeProto::getUuid));
    if (nodeIndex < 0) {
      throw new InvalidRequestException("No node found with Id :" + nodeId);
    }
    return nodes.get(nodeIndex);
  }
}
