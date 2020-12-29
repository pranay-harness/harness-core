package io.harness.serializer.spring.converters.triggers;

import io.harness.pms.contracts.ambiance.TriggeredBy;
import io.harness.serializer.spring.ProtoReadConverter;

public class TriggeredByReadConverter extends ProtoReadConverter<TriggeredBy> {
  public TriggeredByReadConverter() {
    super(TriggeredBy.class);
  }
}
