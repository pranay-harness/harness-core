package io.harness.commandlibrary.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "CommandVersionDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandVersionDTO {
  String commandId;
  String commandStoreId;
  String version;
  String description;
  String yamlContent;

  @Builder(builderMethodName = "newBuilder")
  public CommandVersionDTO(
      String commandId, String commandStoreId, String version, String description, String yamlContent) {
    this.commandId = commandId;
    this.commandStoreId = commandStoreId;
    this.version = version;
    this.description = description;
    this.yamlContent = yamlContent;
  }
}
