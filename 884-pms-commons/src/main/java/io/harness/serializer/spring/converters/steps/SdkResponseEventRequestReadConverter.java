package io.harness.serializer.spring.converters.steps;

import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.serializer.spring.ProtoReadConverter;

public class SdkResponseEventRequestReadConverter extends ProtoReadConverter<SdkResponseEventRequest> {
  public SdkResponseEventRequestReadConverter() {
    super(SdkResponseEventRequest.class);
  }
}
