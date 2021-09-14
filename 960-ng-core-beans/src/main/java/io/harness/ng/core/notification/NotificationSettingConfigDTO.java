/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.notification;

import io.harness.notification.NotificationChannelType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Optional;
import lombok.Data;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SlackConfigDTO.class, name = "SLACK")
      , @JsonSubTypes.Type(value = PagerDutyConfigDTO.class, name = "PAGERDUTY"),
          @JsonSubTypes.Type(value = MicrosoftTeamsConfigDTO.class, name = "MSTEAMS"),
          @JsonSubTypes.Type(value = EmailConfigDTO.class, name = "EMAIL")
    })
public abstract class NotificationSettingConfigDTO {
  protected NotificationChannelType type;

  @JsonIgnore public abstract Optional<String> getSetting();
}
