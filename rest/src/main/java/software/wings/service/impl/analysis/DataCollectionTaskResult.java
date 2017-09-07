package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataCollectionTaskResult implements NotifyResponseData {
  private DataCollectionTaskStatus status;
  private String errorMessage;

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
