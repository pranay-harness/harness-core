/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.pms.resume;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.resume.publisher.NodeResumeEventPublisher;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
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

public class NodeResumeHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private NodeResumeEventPublisher nodeResumeEventPublisher;

  public void resume(NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError) {
    nodeResumeEventPublisher.publishEvent(nodeExecution, buildResponseMap(nodeExecution, responseMap), isError);
  }

  private Map<String, ByteString> buildResponseMap(NodeExecution nodeExecution, Map<String, ByteString> response) {
    Map<String, ByteString> byteResponseMap = new HashMap<>();
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
        byteResponseMap.put(node.getUuid(), ByteString.copyFrom(kryoSerializer.asDeflatedBytes(notifyData)));
      }
      return byteResponseMap;
    }

    return response;
  }

  private boolean accumulationRequired(NodeExecution nodeExecution) {
    ExecutionMode mode = nodeExecution.getMode();
    if (mode != ExecutionMode.CHILD && mode != ExecutionMode.CHILD_CHAIN) {
      return false;
    } else if (mode == ExecutionMode.CHILD) {
      return true;
    } else {
      ChildChainExecutableResponse lastChildChainExecutableResponse = Preconditions.checkNotNull(
          Objects.requireNonNull(nodeExecution.obtainLatestExecutableResponse()).getChildChain());
      return !lastChildChainExecutableResponse.getSuspend();
    }
  }
}
