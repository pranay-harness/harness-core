package io.harness.ngtriggers.beans.dto;

import io.harness.ngtriggers.beans.source.NGTriggerType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("NGTriggerResponse")
@Schema(name = "NGTriggerResponse", description = "This contains the trigger details")
public class NGTriggerResponseDTO {
  String name;
  String identifier;
  String description;
  NGTriggerType type;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String targetIdentifier;
  String yaml;
  @JsonIgnore Long version;
  boolean enabled;
}
