package io.harness.notification.remote.dto;

import io.harness.notification.NotificationChannelType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SlackSettingDTO extends NotificationSettingDTO {
  @Builder
  public SlackSettingDTO(String accountId, String recipient) {
    super(accountId, recipient);
  }

  @Override
  public NotificationChannelType getType() {
    return NotificationChannelType.SLACK;
  }
}
