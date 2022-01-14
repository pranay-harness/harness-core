package io.harness.yaml.snippets.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("YamlSnippetMetaData")
@OwnedBy(DX)
public class YamlSnippetMetaDataDTO {
  String name;
  String description;
  String version;
  /**
   * slug of name and version.
   */
  String identifier;
  List<String> tags;
  String iconTag;
}
