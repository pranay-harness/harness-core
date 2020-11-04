package io.harness.app.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Value
@Builder
public class YAML {
  private String pipelineYAML;
}
