package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;
import software.wings.alerts.AlertType;

/**
 * Created by sgurubelli on 10/18/17.
 */
@Data
@Builder
public class ManualInterventionNeededAlert implements AlertData {
  private String executionId;
  private String stateExecutionInstanceId;
  private String name;
  private String envId;

  @Override
  public boolean matches(AlertData alertData) {
    return stateExecutionInstanceId.equals(((ManualInterventionNeededAlert) alertData).getStateExecutionInstanceId());
  }

  @Override
  public String getTitle() {
    return String.format(AlertType.ManualInterventionNeeded.getTitle(), name);
  }
}
