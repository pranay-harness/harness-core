package io.harness.serializer.json;

import io.harness.pms.contracts.ambiance.TriggeredBy;

public class TriggeredBySerializer extends ProtoJsonSerializer<TriggeredBy> {
  public TriggeredBySerializer() {
    super(TriggeredBy.class);
  }
}
