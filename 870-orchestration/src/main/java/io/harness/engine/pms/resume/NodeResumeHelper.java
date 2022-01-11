/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.resume;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.resume.publisher.NodeResumeEventPublisher;
import io.harness.engine.pms.resume.publisher.ResumeMetadata;
import io.harness.execution.NodeExecution;
import io.harness.plan.Node;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeResumeHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PmsOutcomeService pmsOutcomeService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private NodeResumeEventPublisher nodeResumeEventPublisher;

  public void resume(NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError) {
    ResumeMetadata resumeMetadata = ResumeMetadata.fromNodeExecution(nodeExecution);
    nodeResumeEventPublisher.publishEvent(resumeMetadata, buildResponseMap(resumeMetadata, responseMap), isError);
  }

  private Map<String, ByteString> buildResponseMap(ResumeMetadata resumeMetadata, Map<String, ByteString> response) {
    Map<String, ByteString> byteResponseMap = new HashMap<>();
    if (accumulationRequired(resumeMetadata)) {
      List<NodeExecution> childExecutions =
          nodeExecutionService.fetchNodeExecutionsByParentId(resumeMetadata.getNodeExecutionUuid(), false);
      for (NodeExecution childExecution : childExecutions) {
        Node node = childExecution.getNode();
        StepResponseNotifyData notifyData =
            StepResponseNotifyData.builder()
                .nodeUuid(node.getUuid())
                .identifier(node.getIdentifier())
                .group(node.getGroup())
                .status(childExecution.getStatus())
                .failureInfo(childExecution.getFailureInfo())
                .stepOutcomeRefs(pmsOutcomeService.fetchOutcomeRefs(childExecution.getUuid()))
                .adviserResponse(childExecution.getAdviserResponse())
                .build();
        byteResponseMap.put(node.getUuid(), ByteString.copyFrom(kryoSerializer.asDeflatedBytes(notifyData)));
      }
      return byteResponseMap;
    }

    return response;
  }

  private boolean accumulationRequired(ResumeMetadata nodeExecution) {
    ExecutionMode mode = nodeExecution.getMode();
    if (mode != ExecutionMode.CHILD && mode != ExecutionMode.CHILD_CHAIN) {
      return false;
    } else if (mode == ExecutionMode.CHILD) {
      return true;
    } else {
      ChildChainExecutableResponse lastChildChainExecutableResponse = Preconditions.checkNotNull(
          Objects.requireNonNull(nodeExecution.getLatestExecutableResponse()).getChildChain());
      return !lastChildChainExecutableResponse.getSuspend();
    }
  }
}
