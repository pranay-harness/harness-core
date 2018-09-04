package software.wings.beans.instance.dashboard.service;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.Date;
import java.util.List;

/**
 * @author rktummala on 08/14/17
 */
@Data
@Builder
public class DeploymentHistory {
  private ArtifactSummary artifact;
  private Date deployedAt;
  private String status;
  private EntitySummary triggeredBy;
  private EntitySummary pipeline;
  private EntitySummary workflow;
  private long instanceCount;
  private List<EntitySummary> inframappings;
  private List<EntitySummary> envs;
}
