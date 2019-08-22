package io.harness.batch.processing.integration;

import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;

import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Ec2InstanceInfo;
import io.harness.event.payloads.Ec2Lifecycle;
import io.harness.event.payloads.InstanceState;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.payloads.Lifecycle.EventType;

public interface EcsEventGenerator {
  int INSTANCE_STATE_CODE = 16;
  String INSTANCE_TYPE = "t2.small";
  String INSTANCE_STATE_NAME = "running";

  default PublishedMessage getEc2InstanceInfoMessage(String instanceId, String accountId) {
    InstanceState instanceState =
        InstanceState.newBuilder().setCode(INSTANCE_STATE_CODE).setName(INSTANCE_STATE_NAME).build();

    Ec2InstanceInfo ec2InstanceInfo = Ec2InstanceInfo.newBuilder()
                                          .setInstanceId(instanceId)
                                          .setInstanceType(INSTANCE_TYPE)
                                          .setInstanceState(instanceState)
                                          .build();

    Any payload = Any.pack(ec2InstanceInfo);
    return PublishedMessage.builder()
        .accountId(accountId)
        .data(payload.toByteArray())
        .type(ec2InstanceInfo.getClass().getName())
        .build();
  }

  default PublishedMessage getEc2InstanceLifecycleMessage(
      Timestamp timestamp, EventType eventType, String instanceId, String accountId) {
    Ec2Lifecycle ec2Lifecycle =
        Ec2Lifecycle.newBuilder()
            .setLifecycle(Lifecycle.newBuilder().setInstanceId(instanceId).setType(eventType).setTimestamp(timestamp))
            .build();

    Any payload = Any.pack(ec2Lifecycle);
    return PublishedMessage.builder()
        .accountId(accountId)
        .data(payload.toByteArray())
        .type(ec2Lifecycle.getClass().getName())
        .build();
  }
}
