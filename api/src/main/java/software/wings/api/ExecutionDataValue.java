package software.wings.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionDataValue {
  private String displayName;
  private Object value;
}
