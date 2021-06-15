package io.harness.engine.executables;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@OwnedBy(HarnessTeam.CDC)
public class InvocationHelper {
  private final NodeExecutionService nodeExecutionService;
  private final KryoSerializer kryoSerializer;

  @Inject
  public InvocationHelper(NodeExecutionService nodeExecutionService, KryoSerializer kryoSerializer) {
    this.nodeExecutionService = nodeExecutionService;
    this.kryoSerializer = kryoSerializer;
  }

  public Map<String, byte[]> buildResponseMap(NodeExecution nodeExecution, Map<String, ByteString> response) {
    Map<String, byte[]> byteResponseMap = new HashMap<>();
    if (accumulationRequired(nodeExecution)) {
      List<NodeExecution> childExecutions =
          nodeExecutionService.fetchNodeExecutionsByParentId(nodeExecution.getUuid(), false);
      for (NodeExecution childExecution : childExecutions) {
        PlanNodeProto node = childExecution.getNode();
        StepResponseNotifyData notifyData = StepResponseNotifyData.builder()
                                                .nodeUuid(node.getUuid())
                                                .identifier(node.getIdentifier())
                                                .group(node.getGroup())
                                                .status(childExecution.getStatus())
                                                .failureInfo(childExecution.getFailureInfo())
                                                .stepOutcomeRefs(childExecution.getOutcomeRefs())
                                                .adviserResponse(childExecution.getAdviserResponse())
                                                .build();
        byteResponseMap.put(node.getUuid(), kryoSerializer.asDeflatedBytes(notifyData));
      }
      return byteResponseMap;
    }

    if (isNotEmpty(response)) {
      response.forEach((k, v) -> byteResponseMap.put(k, v.toByteArray()));
    }
    return byteResponseMap;
  }

  private boolean accumulationRequired(NodeExecution nodeExecution) {
    ExecutionMode mode = nodeExecution.getMode();
    if (mode != ExecutionMode.CHILD && mode != ExecutionMode.CHILD_CHAIN) {
      return false;
    } else if (mode == ExecutionMode.CHILD) {
      return true;
    } else {
      ChildChainExecutableResponse lastChildChainExecutableResponse = Preconditions.checkNotNull(
          Objects.requireNonNull(obtainLatestExecutableResponse(nodeExecution)).getChildChain());
      return !lastChildChainExecutableResponse.getSuspend();
    }
  }

  public ExecutableResponse obtainLatestExecutableResponse(NodeExecution nodeExecution) {
    List<ExecutableResponse> executableResponses = nodeExecution.getExecutableResponses();
    if (isEmpty(executableResponses)) {
      return null;
    }
    return executableResponses.get(executableResponses.size() - 1);
  }
}
