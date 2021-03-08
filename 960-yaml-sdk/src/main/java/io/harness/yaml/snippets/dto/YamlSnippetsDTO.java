package io.harness.yaml.snippets.dto;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("YamlSnippets")
public class YamlSnippetsDTO {
  List<YamlSnippetMetaDataDTO> yamlSnippets;
}
