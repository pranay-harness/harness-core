package io.harness.cvng.cdng.beans;

import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class InputSetTemplateRequest {
  String pipelineYaml;
  String templateYaml; // We don't need this in the current implementation but keeping it as part of api in case future
                       // implementation changes.
}
