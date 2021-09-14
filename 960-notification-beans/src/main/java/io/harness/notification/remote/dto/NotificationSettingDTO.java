/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.remote.dto;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.notification.NotificationChannelType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.inject.Inject;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = EmailSettingDTO.class, name = "EMAIL")
  , @JsonSubTypes.Type(value = MSTeamSettingDTO.class, name = "MSTEAMS"),
      @JsonSubTypes.Type(value = SlackSettingDTO.class, name = "SLACK"),
      @JsonSubTypes.Type(value = PagerDutySettingDTO.class, name = "PAGERDUTY"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class NotificationSettingDTO {
  @NotNull String accountId;
  @NotNull String recipient;
  @NotNull String notificationId;

  public NotificationSettingDTO(String accountId, String recipient) {
    this.accountId = accountId;
    this.recipient = recipient;
    this.notificationId = generateUuid();
  }

  public abstract NotificationChannelType getType();
}
