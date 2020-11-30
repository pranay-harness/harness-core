package io.harness.ng.core.entities;

import static io.harness.notification.NotificationChannelType.EMAIL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("EMAIL")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailConfig extends NotificationSettingConfig {
  String groupEmail;

  @Builder
  public EmailConfig(String groupEmail) {
    this.groupEmail = groupEmail;
    this.type = EMAIL;
  }
}
