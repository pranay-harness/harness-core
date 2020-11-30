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
@JsonTypeName("PAGERDUTY")
public class PagerDutyConfigDTO extends NotificationSettingConfigDTO {
  @NotNull String pagerDutyKey;

  @Builder
  public PagerDutyConfigDTO(String pagerDutyKey) {
    this.pagerDutyKey = pagerDutyKey;
    this.type = NotificationChannelType.PAGERDUTY;
  }

  @Override
  public Optional<String> getSetting() {
    return Optional.ofNullable(pagerDutyKey);
  }
}
