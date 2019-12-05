package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;

import java.util.List;

/**
 * This is used as request for capturing deployment and instance information.
 * @author rktummala on 02/04/18
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "deploymentEventQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DeploymentEvent extends Queuable {
  private boolean isRollback;
  private List<DeploymentSummary> deploymentSummaries;
  private OnDemandRollbackInfo onDemandRollbackInfo;
}
