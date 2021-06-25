package io.harness.pms.events.base;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public enum PmsEventCategory {
  INTERRUPT_EVENT,
  ORCHESTRATION_EVENT,
  FACILITATOR_EVENT,
  NODE_START,
  PROGRESS_EVENT,
  NODE_ADVISE,
  NODE_RESUME,
  SDK_INTERRUPT_EVENT_NOTIFY,
  SDK_RESPONSE_EVENT
}
