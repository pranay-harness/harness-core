/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.remote.mappers;

import io.harness.notification.entities.NotificationSetting;
import io.harness.notification.remote.dto.AccountNotificationSettingDTO;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationSettingMapper {
  public static Optional<AccountNotificationSettingDTO> toDTO(NotificationSetting notificationSetting) {
    if (!Optional.ofNullable(notificationSetting).isPresent()) {
      return Optional.empty();
    }
    return Optional.of(AccountNotificationSettingDTO.builder()
                           .accountId(notificationSetting.getAccountId())
                           .sendNotificationViaDelegate(notificationSetting.isSendNotificationViaDelegate())
                           .smtpConfig(notificationSetting.getSmtpConfig())
                           .build());
  }
}
