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
