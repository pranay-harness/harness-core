package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;

public class SdkResponseEventRequestMorphiaConverter extends ProtoMessageConverter<SdkResponseEventRequest> {
  public SdkResponseEventRequestMorphiaConverter() {
    super(SdkResponseEventRequest.class);
  }
}
