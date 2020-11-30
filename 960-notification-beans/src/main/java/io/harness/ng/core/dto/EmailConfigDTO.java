package io.harness.ng.core.dto;

import io.harness.notification.NotificationChannelType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("EMAIL")
public class EmailConfigDTO extends NotificationSettingConfigDTO {
  @NotNull String groupEmail;

  @Builder
  public EmailConfigDTO(String groupEmail) {
    this.groupEmail = groupEmail;
    this.type = NotificationChannelType.EMAIL;
  }

  @Override
  public Optional<String> getSetting() {
    return Optional.ofNullable(groupEmail);
  }
}