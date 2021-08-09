package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.plan.TriggeredBy;

public class TriggeredByMorphiaConverter extends ProtoMessageConverter<TriggeredBy> {
  public TriggeredByMorphiaConverter() {
    super(TriggeredBy.class);
  }
}
