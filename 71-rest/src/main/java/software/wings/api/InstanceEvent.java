package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;

import java.util.Set;

/**
 * This is used as request for capturing instance information.
 * @author rktummala on 08/05/19
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "instanceEventQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class InstanceEvent extends Queuable {
  private String accountId;
  private Set<String> deletions;
  private long deletionTimestamp;
  private Set<Instance> insertions;
  private TimeSeriesBatchEventInfo timeSeriesBatchEventInfo;
}
