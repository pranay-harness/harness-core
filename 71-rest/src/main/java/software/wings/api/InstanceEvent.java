package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.infrastructure.instance.Instance;

import java.util.Set;

/**
 * This is used as request for capturing instance information.
 * @author rktummala on 08/05/19
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "instanceEventQueue", noClassnameStored = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class InstanceEvent extends Queuable {
  private String accountId;
  private Set<String> deletions;
  private long deletionTimestamp;
  private Set<Instance> insertions;
}
