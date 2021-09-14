/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.webhook;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class WebhookConstants {
  public static final String TARGET = "target";
  public static final String HOOK_EVENT_TYPE = "hookEventType";
  public static final String WEBHOOK_ENDPOINT = "webhook";
}
