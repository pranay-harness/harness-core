/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.pms.resume.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.resume.ChainDetails;
import io.harness.pms.contracts.resume.NodeResumeEvent;
import io.harness.pms.events.base.PmsEventCategory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class RedisNodeResumeEventPublisher implements NodeResumeEventPublisher {
  @Inject private PmsEventSender eventSender;

  @Override
  public void publishEvent(NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError) {
    String serviceName = nodeExecution.getNode().getServiceName();
    NodeResumeEvent.Builder resumeEventBuilder = NodeResumeEvent.newBuilder()
                                                     .setAmbiance(nodeExecution.getAmbiance())
                                                     .setExecutionMode(nodeExecution.getMode())
                                                     .setStepParameters(nodeExecution.getResolvedStepParametersBytes())
                                                     .addAllRefObjects(nodeExecution.getNode().getRebObjectsList())
                                                     .setAsyncError(isError)
                                                     .putAllResponse(responseMap);

    ChainDetails chainDetails = buildChainDetails(nodeExecution);
    if (chainDetails != null) {
      resumeEventBuilder.setChainDetails(chainDetails);
    }

    eventSender.sendEvent(nodeExecution.getAmbiance(), resumeEventBuilder.build().toByteString(),
        PmsEventCategory.NODE_RESUME, serviceName, true);
  }

  public ChainDetails buildChainDetails(NodeExecution nodeExecution) {
    ExecutionMode mode = nodeExecution.getMode();

    if (mode == ExecutionMode.TASK_CHAIN || mode == ExecutionMode.CHILD_CHAIN) {
      switch (mode) {
        case TASK_CHAIN:
          TaskChainExecutableResponse lastLinkResponse =
              Objects.requireNonNull(nodeExecution.obtainLatestExecutableResponse()).getTaskChain();
          return ChainDetails.newBuilder()
              .setIsEnd(lastLinkResponse.getChainEnd())
              .setPassThroughData(lastLinkResponse.getPassThroughData())
              .build();
        case CHILD_CHAIN:
          ChildChainExecutableResponse lastChildChainExecutableResponse = Preconditions.checkNotNull(
              Objects.requireNonNull(nodeExecution.obtainLatestExecutableResponse()).getChildChain());
          boolean chainEnd =
              lastChildChainExecutableResponse.getLastLink() || lastChildChainExecutableResponse.getSuspend();
          return ChainDetails.newBuilder()
              .setIsEnd(chainEnd)
              .setPassThroughData(lastChildChainExecutableResponse.getPassThroughData())
              .build();
        default:
          log.error("This Should Not Happen not a chain mode");
      }
    }
    return null;
  }
}
