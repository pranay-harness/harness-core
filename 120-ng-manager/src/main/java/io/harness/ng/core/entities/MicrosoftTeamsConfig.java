package io.harness.ng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("MicrosoftTeams")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MicrosoftTeamsConfig extends NotificationSettingConfig {
  String microsoftTeamsWebhookUrl;
}