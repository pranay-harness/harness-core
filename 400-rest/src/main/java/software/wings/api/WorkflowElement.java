package software.wings.api;

import software.wings.beans.ArtifactVariable;
import software.wings.beans.ManifestVariable;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

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
  private String pipelineResumeUuid;
  private Long startTs;
  private List<ArtifactVariable> artifactVariables;
  private List<ManifestVariable> manifestVariables;
}
