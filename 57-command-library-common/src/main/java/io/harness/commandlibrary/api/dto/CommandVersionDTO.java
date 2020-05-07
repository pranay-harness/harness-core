package io.harness.commandlibrary.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "CommandVersionDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandVersionDTO {
  String commandName;
  String commandStoreName;
  String version;
  String description;
  Set<String> tags;
  String repoUrl;

  @Builder(builderMethodName = "newBuilder")
  public CommandVersionDTO(String commandName, String commandStoreName, String version, String description,
      Set<String> tags, String repoUrl) {
    this.commandName = commandName;
    this.commandStoreName = commandStoreName;
    this.version = version;
    this.description = description;
    this.tags = tags;
    this.repoUrl = repoUrl;
  }
}
