package software.wings.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDC)
public class ExecutionDataValue {
  private String displayName;
  private Object value;

  /**
   * Static Factory Method
   * @param displayName
   * @param value
   * @return new ExecutionDataValue
   */
  public static ExecutionDataValue executionDataValue(String displayName, Object value) {
    return new ExecutionDataValue(displayName, value);
  }
}
