package io.harness.beans.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@OwnedBy(CDC)
@Data
public class TestEventPayload extends EventPayloadData {
  private String message = "This is a test payload!";
}
