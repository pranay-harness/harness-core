/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.remote.dto;

import io.harness.notification.SmtpConfig;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountNotificationSettingDTO {
  @NotNull String accountId;
  Boolean sendNotificationViaDelegate;
  SmtpConfig smtpConfig;
}
