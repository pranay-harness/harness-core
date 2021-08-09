package io.harness.serializer.json;

import io.harness.pms.contracts.plan.TriggeredBy;

public class TriggeredBySerializer extends ProtoJsonSerializer<TriggeredBy> {
  public TriggeredBySerializer() {
    super(TriggeredBy.class);
  }
}
