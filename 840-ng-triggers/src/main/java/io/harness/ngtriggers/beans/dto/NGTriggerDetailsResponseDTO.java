package io.harness.ngtriggers.beans.dto;

import io.harness.ngtriggers.beans.source.NGTriggerType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
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
public class NGTriggerDetailsResponseDTO {
  String name;
  String identifier;
  String description;
  NGTriggerType type;
  LastTriggerExecutionDetails lastTriggerExecutionDetails;
  WebhookDetails webhookDetails;
  Map<String, String> tags;
  boolean enabled;
}