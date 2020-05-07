package io.harness.commandlibrary.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CommandDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandDTO {
  String commandStoreName;
  String type;
  String name;
  String description;
  String imageUrl;
  CommandVersionDTO latestVersion;
  List<CommandVersionDTO> versionList;
  Set<String> tags;
  String repoUrl;
}
