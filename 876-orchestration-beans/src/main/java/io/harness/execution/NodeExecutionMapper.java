/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;
import io.harness.serializer.ProtoUtils;

import java.util.Collections;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class NodeExecutionMapper {
  public NodeExecution fromNodeExecutionProto(NodeExecutionProto proto) {
    return NodeExecution.builder()
        .uuid(proto.getUuid())
        .ambiance(proto.getAmbiance())
        .node(proto.getNode())
        .mode(proto.getMode())
        .startTs(ProtoUtils.timestampToUnixMillis(proto.getStartTs()))
        .endTs(ProtoUtils.timestampToUnixMillis(proto.getEndTs()))
        .resolvedStepInputs(proto.getResolvedStepInputs())
        .initialWaitDuration(ProtoUtils.durationToJavaDuration(proto.getInitialWaitDuration()))
        .resolvedStepParameters(proto.getResolvedStepParameters())
        .notifyId(proto.getNotifyId())
        .parentId(proto.getParentId())
        .nextId(proto.getParentId())
        .previousId(proto.getPreviousId())
        .status(proto.getStatus())
        .executableResponses(CollectionUtils.emptyIfNull(proto.getExecutableResponsesList()))
        .interruptHistories(Collections.emptyList())
        .failureInfo(proto.getFailureInfo())
        .retryIds(Collections.emptyList())
        .oldRetry(false)
        .timeoutInstanceIds(Collections.emptyList())
        .timeoutDetails(null)
        .retryIds(proto.getRetryIdsList())
        .oldRetry(proto.getOldRetry())
        .interruptHistories(proto.getInterruptHistoriesList()
                                .stream()
                                .map(NodeExecutionMapper::fromInterruptEffectProto)
                                .collect(Collectors.toList()))
        .build();
  }

  private InterruptEffect fromInterruptEffectProto(InterruptEffectProto interruptEffectProto) {
    return InterruptEffect.builder()
        .interruptId(interruptEffectProto.getInterruptId())
        .interruptType(interruptEffectProto.getInterruptType())
        .interruptConfig(interruptEffectProto.getInterruptConfig())
        .tookEffectAt(ProtoUtils.timestampToUnixMillis(interruptEffectProto.getTookEffectAt()))
        .build();
  }

  private InterruptEffectProto toInterruptEffect(InterruptEffect interruptEffect) {
    return InterruptEffectProto.newBuilder()
        .setTookEffectAt(ProtoUtils.unixMillisToTimestamp(interruptEffect.getTookEffectAt()))
        .setInterruptConfig(interruptEffect.getInterruptConfig())
        .setInterruptId(interruptEffect.getInterruptId())
        .setInterruptType(interruptEffect.getInterruptType())
        .build();
  }
}
