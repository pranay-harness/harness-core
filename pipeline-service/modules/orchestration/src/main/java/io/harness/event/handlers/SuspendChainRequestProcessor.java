/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SuspendChainRequest;
import io.harness.pms.contracts.resume.ResponseDataProto;
import io.harness.pms.execution.utils.SdkResponseEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class SuspendChainRequestProcessor implements SdkResponseProcessor {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    SuspendChainRequest request = event.getSuspendChainRequest();
    nodeExecutionService.updateV2(SdkResponseEventUtils.getNodeExecutionId(event),
        ops -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()));
    Map<String, ResponseDataProto> responseDataProtoMap = new HashMap<>();
    request.getResponseMap().forEach(
        (k, v)
            -> responseDataProtoMap.put(
                k, ResponseDataProto.newBuilder().setResponse(v).setUsingKryoWithoutReference(false).build()));
    engine.resumeNodeExecution(event.getAmbiance(), responseDataProtoMap, request.getIsError());
  }
}
