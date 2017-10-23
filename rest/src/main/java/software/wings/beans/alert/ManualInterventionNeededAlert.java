package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;

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
    ManualInterventionNeededAlert manualInterventionNeededAlert = (ManualInterventionNeededAlert) alertData;
    return stateExecutionInstanceId == null
        ? executionId.equals(manualInterventionNeededAlert.getExecutionId())
        : stateExecutionInstanceId.equals(manualInterventionNeededAlert.getStateExecutionInstanceId());
  }

  @Override
  public String buildTitle() {
    return name + " requires manual action";
  }
}
