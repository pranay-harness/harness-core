package software.wings.api;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class PipelineElement {
  private String name;
  private String displayName;
  private String description;
  private Long startTs;
}
