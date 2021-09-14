/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entities;

import static io.harness.notification.NotificationChannelType.PAGERDUTY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("PAGERDUTY")
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PL)
public class PagerDutyConfig extends NotificationSettingConfig {
  String pagerDutyKey;

  @Builder
  public PagerDutyConfig(String pagerDutyKey) {
    this.pagerDutyKey = pagerDutyKey;
    this.type = PAGERDUTY;
  }
}
