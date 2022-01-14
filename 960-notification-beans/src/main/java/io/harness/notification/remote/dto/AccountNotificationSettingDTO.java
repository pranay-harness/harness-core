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
