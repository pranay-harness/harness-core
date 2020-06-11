package software.wings.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
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
