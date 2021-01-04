package software.wings.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelineElement {
  private String name;
  private String displayName;
  private String description;
  private Long startTs;
}
