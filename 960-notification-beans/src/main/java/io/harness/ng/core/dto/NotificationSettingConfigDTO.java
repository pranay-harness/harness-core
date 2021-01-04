package io.harness.ng.core.dto;

import io.harness.ng.core.dto.EmailConfigDTO;
import io.harness.ng.core.dto.MicrosoftTeamsConfigDTO;
import io.harness.ng.core.dto.PagerDutyConfigDTO;
import io.harness.ng.core.dto.SlackConfigDTO;
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
