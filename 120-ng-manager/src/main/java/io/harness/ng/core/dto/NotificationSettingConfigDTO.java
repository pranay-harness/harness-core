package io.harness.ng.core.dto;

import io.harness.ng.core.entities.NotificationSettingType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SlackConfigDTO.class, name = "Slack")
      , @JsonSubTypes.Type(value = PagerDutyConfigDTO.class, name = "PagerDuty"),
    })
public abstract class NotificationSettingConfigDTO {
  private NotificationSettingType type;
}
