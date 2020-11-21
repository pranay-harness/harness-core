package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Slack")
public class SlackConfigDTO extends NotificationSettingConfigDTO {
  @NotNull String slackId;
}
