package software.wings.service.impl.analysis;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.sm.StateType;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataCollectionTaskResult implements ResponseData {
  private DataCollectionTaskStatus status;
  private String errorMessage;
  private StateType stateType;

  // State specific results.....

  // NewRelicDeploymentMarker state
  private String newRelicDeploymentMarkerBody;

  public enum DataCollectionTaskStatus {
    /**
     * Success execution status.
     */
    SUCCESS,
    /**
     * Failure execution status.
     */
    FAILURE;
  }
}
