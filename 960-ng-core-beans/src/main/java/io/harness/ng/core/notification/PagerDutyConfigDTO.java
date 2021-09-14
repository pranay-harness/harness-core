/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.notification;

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
