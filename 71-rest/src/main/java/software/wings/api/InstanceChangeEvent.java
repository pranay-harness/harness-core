package software.wings.api;

import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.infrastructure.instance.Instance;

import java.util.List;

/**
 * This is a wrapper class of Instance to make it extend queuable.
 * This is used as request for capturing instance information.
 * @author rktummala on 08/24/17
 *
 */
@Entity(value = "instanceChangeQueue", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class InstanceChangeEvent extends Queuable {
  private List<Instance> instanceList;
  private List<String> autoScalingGroupList;
  private String appId;
}
