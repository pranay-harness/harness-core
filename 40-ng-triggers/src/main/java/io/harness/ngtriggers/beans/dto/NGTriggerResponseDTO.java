package io.harness.ngtriggers.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.swagger.annotations.ApiModel;
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
}
