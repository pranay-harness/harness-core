/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.notification;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface SlackNotificationConfiguration {
  /**
   * We just need a webhook URL to send a slack message. So, this is optional.
   */
  @Nullable String getName();

  @NotNull String getOutgoingWebhookUrl();
}
