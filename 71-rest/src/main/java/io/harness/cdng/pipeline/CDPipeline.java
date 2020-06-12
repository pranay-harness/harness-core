package io.harness.cdng.pipeline;

import io.harness.yaml.core.Tag;
import io.harness.yaml.core.auxiliary.intfc.StageWrapper;
import io.harness.yaml.core.intfc.Pipeline;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CDPipeline implements Pipeline {
  private String displayName;
  private String description;
  private List<Tag> tags;
  @Singular private List<StageWrapper> stages;
  private String identifier;
}
