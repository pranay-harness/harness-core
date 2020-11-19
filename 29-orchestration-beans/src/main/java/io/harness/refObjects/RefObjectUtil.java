package io.harness.refObjects;

import io.harness.pms.refobjects.RefObject;
import io.harness.pms.refobjects.RefType;
import io.harness.references.OrchestrationRefType;
import lombok.experimental.UtilityClass;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

@UtilityClass
public class RefObjectUtil {
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
