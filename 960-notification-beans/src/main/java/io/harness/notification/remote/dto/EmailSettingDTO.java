/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
