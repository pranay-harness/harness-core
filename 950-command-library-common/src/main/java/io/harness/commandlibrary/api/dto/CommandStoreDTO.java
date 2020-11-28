package io.harness.commandlibrary.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CommandStoreKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandStoreDTO {
  String name;
  String displayName;
  String description;
}
