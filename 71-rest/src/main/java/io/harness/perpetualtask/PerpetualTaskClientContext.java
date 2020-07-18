package io.harness.perpetualtask;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Map;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PerpetualTaskClientContextKeys")
public class PerpetualTaskClientContext {
  // This is a set of arbitrary client parameters that will allow for the task to be identified from the
  // client that requested it and will provide the necessary task parameters.
  private Map<String, String> clientParams;

  // Alternatively the caller might provide the task parameters directly to be stored with the task.
  // In this case we are not going to make a request back for them.
  private byte[] executionBundle;

  // Last time the context was updated.
  private long lastContextUpdated;
}
