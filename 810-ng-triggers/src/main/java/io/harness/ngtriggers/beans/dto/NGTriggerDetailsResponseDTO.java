package io.harness.ngtriggers.beans.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.metadata.WebhookRegistrationStatus;
import io.harness.ngtriggers.beans.entity.metadata.status.TriggerStatus;
import io.harness.ngtriggers.beans.source.NGTriggerType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("NGTriggerDetailsResponse")
@OwnedBy(PIPELINE)
public class NGTriggerDetailsResponseDTO {
  String name;
  String identifier;
  String description;
  NGTriggerType type;
  TriggerStatus triggerStatus;
  LastTriggerExecutionDetails lastTriggerExecutionDetails;
  WebhookDetails webhookDetails;
  BuildDetails buildDetails;
  Map<String, String> tags;
  List<Integer> executions;
  String yaml;
  String webhookUrl;
  WebhookRegistrationStatus registrationStatus;
  boolean enabled;
}