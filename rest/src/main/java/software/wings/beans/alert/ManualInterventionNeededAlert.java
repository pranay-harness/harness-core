package software.wings.beans.alert;

import com.google.inject.Injector;

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
  public boolean matches(AlertData alertData, Injector injector) {
    return stateExecutionInstanceId.equals(((ManualInterventionNeededAlert) alertData).getStateExecutionInstanceId());
  }

  @Override
  public String buildTitle(Injector injector) {
    return name + " requires manual action";
  }
}
