package io.harness.cdng.pipeline;

import io.harness.yaml.core.Tag;
import io.harness.yaml.core.intfc.Pipeline;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class CDPipeline implements Pipeline {
  private String name;
  private String description;
  private List<Tag> tags;
  @Singular private List<CDStage> stages;
  private String identifier;
}
