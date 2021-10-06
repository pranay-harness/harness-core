package software.wings.beans.instance.dashboard.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.appmanifest.ManifestSummary;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.Date;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/14/17
 */
@Data
@Builder
@OwnedBy(DX)
public class CurrentActiveInstances {
  private EntitySummary environment;
  private long instanceCount;
  private ArtifactSummary artifact;
  private ManifestSummary manifest;
  private EntitySummary serviceInfra;
  private EntitySummary workflow;
  private Date lastWorkflowExecutionDate;
  private Date deployedAt;
  private EntitySummary lastWorkflowExecution;
  private boolean onDemandRollbackAvailable;
}
