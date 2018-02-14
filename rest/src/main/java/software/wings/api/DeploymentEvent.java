package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

/**
 * This is used as request for capturing deployment and instance information.
 * @author rktummala on 02/04/18
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "deploymentEventQueue", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class DeploymentEvent extends Queuable {
  @Embedded private DeploymentInfo deploymentInfo;

  @Builder
  public DeploymentEvent(DeploymentInfo deploymentInfo) {
    this.deploymentInfo = deploymentInfo;
  }
}
