/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
