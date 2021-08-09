package io.harness.pms.sdk.core.resolver;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.data.OrchestrationRefType;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RefObjectUtils {
  private final String PRODUCER_ID = "__PRODUCER_ID__";

  public RefObject getOutcomeRefObject(String name, String producerId, String key) {
    if (isEmpty(key)) {
      key = name;
    }
    return RefObject.newBuilder()
        .setName(name)
        .setProducerId(producerId)
        .setKey(key)
        .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
        .build();
  }

  public RefObject getOutcomeRefObject(String name) {
    return RefObject.newBuilder()
        .setName(name)
        .setKey(name)
        .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
        .build();
  }

  public RefObject getSweepingOutputRefObject(String name, String producerId, String key) {
    if (producerId == null) {
      producerId = PRODUCER_ID;
    }
    return RefObject.newBuilder()
        .setName(name)
        .setProducerId(producerId)
        .setKey(key)
        .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
        .build();
  }

  public RefObject getSweepingOutputRefObject(String name) {
    return RefObject.newBuilder()
        .setName(name)
        .setKey(name)
        .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
        .build();
  }
}
