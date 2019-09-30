package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

/**
 * This event is used for capturing deployment information.
 * @author rktummala on 08/12/19
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "deploymentTimeSeriesEventQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DeploymentTimeSeriesEvent extends Queuable {
  private TimeSeriesEventInfo timeSeriesEventInfo;
}
