package io.harness.ng.core.entities;

import static io.harness.notification.NotificationChannelType.PAGERDUTY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("PAGERDUTY")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagerDutyConfig extends NotificationSettingConfig {
  String pagerDutyKey;

  @Builder
  public PagerDutyConfig(String pagerDutyKey) {
    this.pagerDutyKey = pagerDutyKey;
    this.type = PAGERDUTY;
  }
}
