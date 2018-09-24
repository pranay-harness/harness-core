package software.wings.beans.instance.dashboard.service;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.Date;

/**
 * @author rktummala on 08/14/17
 */
@Data
@Builder
public class CurrentActiveInstances {
  private EntitySummary environment;
  private long instanceCount;
  private ArtifactSummary artifact;
  private EntitySummary serviceInfra;
  private Date deployedAt;
}
