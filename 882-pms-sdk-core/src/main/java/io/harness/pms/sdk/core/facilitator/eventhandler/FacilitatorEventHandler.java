package io.harness.pms.sdk.core.facilitator.eventhandler;

import io.harness.pms.contracts.facilitators.FacilitatorEvent;

public interface FacilitatorEventHandler {
  boolean handleEvent(FacilitatorEvent event);
}
