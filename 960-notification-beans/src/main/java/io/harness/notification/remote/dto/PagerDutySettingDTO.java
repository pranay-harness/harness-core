/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.remote.dto;

import io.harness.notification.NotificationChannelType;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PagerDutySettingDTO extends NotificationSettingDTO {
  @Builder
  public PagerDutySettingDTO(String accountId, String recipient) {
    super(accountId, recipient);
  }

  @Override
  public NotificationChannelType getType() {
    return NotificationChannelType.PAGERDUTY;
  }
}
