/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entities;

import static io.harness.notification.NotificationChannelType.MSTEAMS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("MSTEAMS")
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PL)
public class MicrosoftTeamsConfig extends NotificationSettingConfig {
  String microsoftTeamsWebhookUrl;

  @Builder
  public MicrosoftTeamsConfig(String microsoftTeamsWebhookUrl) {
    this.microsoftTeamsWebhookUrl = microsoftTeamsWebhookUrl;
    this.type = MSTEAMS;
  }
}
