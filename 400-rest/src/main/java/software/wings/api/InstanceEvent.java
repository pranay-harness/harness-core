package software.wings.api;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.ng.DbAliases;
import io.harness.queue.Queuable;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

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
@StoreIn(DbAliases.CG_MANAGER)
public class InstanceEvent extends Queuable {
  private String accountId;
  private Set<String> deletions;
  private long deletionTimestamp;
  private Set<Instance> insertions;
  private TimeSeriesBatchEventInfo timeSeriesBatchEventInfo;
}
