package io.harness.commandlibrary.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CommandDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandDTO {
  String id;
  String commandStoreId;
  String type;
  String name;
  String description;
  String category;
  String imageUrl;
  CommandVersionDTO latestVersion;
  List<CommandVersionDTO> versionList;
}
