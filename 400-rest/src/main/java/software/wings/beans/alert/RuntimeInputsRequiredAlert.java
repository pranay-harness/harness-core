package software.wings.beans.alert;

import io.harness.alert.AlertData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuntimeInputsRequiredAlert implements AlertData {
  private String executionId;
  private String stateExecutionInstanceId;
  private String name;
  private String envId;

  @Override
  public boolean matches(AlertData alertData) {
    return executionId.equals(((RuntimeInputsRequiredAlert) alertData).getExecutionId());
  }

  @Override
  public String buildTitle() {
    return name + " requires runtime inputs";
  }
}
