/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.AbortExpireTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.CreatePerpetualTaskRequestClassic;
import io.harness.delegate.CreatePerpetualTaskResponseClassic;
import io.harness.delegate.DelegateClassicTaskRequest;
import io.harness.delegate.DelegateTaskGrpc;
import io.harness.delegate.ExecuteTaskResponse;
import io.harness.delegate.ExpireTaskResponse;
import io.harness.delegate.QueueTaskResponse;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.serializer.KryoSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceClassicGrpcClient {
  private final DelegateTaskGrpc.DelegateTaskBlockingStub delegateTaskBlockingStub;
  private final KryoSerializer kryoSerializer;

  @Inject
  public DelegateServiceClassicGrpcClient(
      DelegateTaskGrpc.DelegateTaskBlockingStub blockingStub, KryoSerializer kryoSerializer) {
    this.delegateTaskBlockingStub = blockingStub;
    this.kryoSerializer = kryoSerializer;
  }

  public String queueTask(DelegateTask task) {
    ByteString.copyFrom(kryoSerializer.asDeflatedBytes(task));
    DelegateClassicTaskRequest delegateClassicTaskRequest =
        DelegateClassicTaskRequest.newBuilder()
            .setDelegateTaskKryo(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(task)))
            .build();
    final QueueTaskResponse queueTaskResponse = delegateTaskBlockingStub.queueTask(delegateClassicTaskRequest);
    return queueTaskResponse.getUuid();
  }

  public <T extends DelegateResponseData> T executeTask(DelegateTask task) {
    DelegateClassicTaskRequest delegateClassicTaskRequest =
        DelegateClassicTaskRequest.newBuilder()
            .setDelegateTaskKryo(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(task)))
            .build();

    final ExecuteTaskResponse executeTaskResponse = delegateTaskBlockingStub.executeTask(delegateClassicTaskRequest);
    ObjectMapper mapper = new ObjectMapper();
    return (T) mapper.convertValue(
        kryoSerializer.asInflatedObject(executeTaskResponse.getDelegateTaskResponseKryo().toByteArray()),
        DelegateResponseData.class);
  }

  public DelegateTask abortTask(String accountId, String delegateTaskId) {
    final AbortTaskResponse abortTaskResponse = delegateTaskBlockingStub.abortTask(
        AbortExpireTaskRequest.newBuilder().setAccountId(accountId).setDelegateTaskId(delegateTaskId).build());
    return (DelegateTask) kryoSerializer.asInflatedObject(abortTaskResponse.getDelegateTaskKryo().toByteArray());
  }

  public String expireTask(String accountId, String delegateTaskId) {
    final ExpireTaskResponse expireTaskResponse = delegateTaskBlockingStub.expireTask(
        AbortExpireTaskRequest.newBuilder().setAccountId(accountId).setDelegateTaskId(delegateTaskId).build());
    return (String) kryoSerializer.asInflatedObject(expireTaskResponse.getMessageBytes().toByteArray());
  }

  public String createPerpetualTask(String perpetualTaskType, String accountId,
      PerpetualTaskClientContext clientContext, PerpetualTaskSchedule schedule, boolean allowDuplicate,
      String taskDescription) {
    final CreatePerpetualTaskResponseClassic createPerpetualTaskResponse =
        delegateTaskBlockingStub.createPerpetualTaskClassic(
            CreatePerpetualTaskRequestClassic.newBuilder()
                .setPerpetualTaskType(perpetualTaskType)
                .setAccountId(accountId)
                .setPerpetualTaskScheduleKryo(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(schedule)))
                .setClientContextKryo(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(clientContext)))
                .setAllowDuplicate(allowDuplicate)
                .setTaskDescription(taskDescription)
                .build());
    return createPerpetualTaskResponse.getMessage();
  }
}
