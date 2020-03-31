package io.harness.commandlibrary.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@NoArgsConstructor
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

  @Builder
  public CommandDTO(String id, String commandStoreId, String type, String name, String description, String category,
      String imageUrl, CommandVersionDTO latestVersion, List<CommandVersionDTO> versionList) {
    this.id = id;
    this.commandStoreId = commandStoreId;
    this.type = type;
    this.name = name;
    this.description = description;
    this.category = category;
    this.imageUrl = imageUrl;
    this.latestVersion = latestVersion;
    this.versionList = versionList;
  }
}
