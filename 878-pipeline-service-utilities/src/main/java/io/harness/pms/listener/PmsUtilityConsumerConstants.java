package io.harness.pms.listener;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsUtilityConsumerConstants {
  public static final String PT_INTERRUPT_LISTENER = "INTERRUPT_LISTENER";
  public static final String PT_INTERRUPT_CONSUMER = "INTERRUPT_CONSUMER";

  public static final String PT_FACILITATOR_LISTENER = "FACILITATOR_LISTENER";
  public static final String PT_FACILITATOR_CONSUMER = "FACILITATOR_CONSUMER";

  public static final String PT_NODE_START_LISTENER = "NODE_START_LISTENER";
  public static final String PT_NODE_START_CONSUMER = "NODE_START_CONSUMER";

  public static final String PT_PROGRESS_LISTENER = "PROGRESS_LISTENER";
  public static final String PT_PROGRESS_CONSUMER = "PROGRESS_CONSUMER";

  public static final String PT_NODE_ADVISE_LISTENER = "NODE_ADVISE_LISTENER";
  public static final String PT_NODE_ADVISE_CONSUMER = "NODE_ADVISE_CONSUMER";

  public static final String PT_NODE_RESUME_LISTENER = "NODE_RESUME_LISTENER";
  public static final String PT_NODE_RESUME_CONSUMER = "NODE_RESUME_CONSUMER";

  public static final String PT_ORCHESTRATION_EVENT_CONSUMER = "ORCHESTRATION_EVENT_CONSUMER";
  public static final String PT_ORCHESTRATION_EVENT_LISTENER = "ORCHESTRATION_EVENT_LISTENER";
}
