package io.harness.ng.webhook;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class WebhookConstants {
  public static final String TARGET = "target";
  public static final String HOOK_EVENT_TYPE = "hookEventType";
}
