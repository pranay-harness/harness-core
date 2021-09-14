/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.remote.dto;

import io.harness.notification.NotificationChannelType;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmailSettingDTO extends NotificationSettingDTO {
  @NotNull private String subject;
  @NotNull private String body;

  @Builder
  public EmailSettingDTO(String accountId, String recipient, String subject, String body) {
    super(accountId, recipient);
    this.subject = subject;
    this.body = body;
  }

  @Override
  public NotificationChannelType getType() {
    return NotificationChannelType.EMAIL;
  }
}
