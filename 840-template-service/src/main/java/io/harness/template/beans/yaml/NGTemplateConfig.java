package io.harness.template.beans.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@TypeAlias("ngTemplateConfig")
public class NGTemplateConfig implements YamlDTO {
  @JsonProperty("template") NGTemplateInfoConfig templateInfoConfig;
}
