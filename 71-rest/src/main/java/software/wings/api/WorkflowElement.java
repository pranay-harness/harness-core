package software.wings.api;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class WorkflowElement {
  private String uuid;
  private String name;
  private String displayName;
  private String description;
  private String releaseNo;
  private String url;
  private Map<String, Object> variables;
  private String lastGoodDeploymentUuid;
  private String lastGoodDeploymentDisplayName;
  private String lastGoodReleaseNo;
  private String pipelineDeploymentUuid;
  private Long startTs;
}
