package io.harness.commandlibrary.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants(innerTypeName = "CommandStoreKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandStoreDTO {
  String id;
  String name;
  String description;

  @Builder
  public CommandStoreDTO(String id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }
}
