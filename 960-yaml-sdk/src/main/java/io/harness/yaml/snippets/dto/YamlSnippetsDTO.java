package io.harness.yaml.snippets.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("YamlSnippets")
@OwnedBy(DX)
@Schema(name = "YamlSnippets", description = "This is the view of the YamlSnippets entity defined in Harness")
public class YamlSnippetsDTO {
  List<YamlSnippetMetaDataDTO> yamlSnippets;
}
