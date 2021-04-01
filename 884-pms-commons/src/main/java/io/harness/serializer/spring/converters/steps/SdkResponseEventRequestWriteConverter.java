package io.harness.serializer.spring.converters.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.serializer.spring.ProtoWriteConverter;

@OwnedBy(PIPELINE)
public class SdkResponseEventRequestWriteConverter extends ProtoWriteConverter<SdkResponseEventRequest> {}
