package inputset;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ManualExecutionInputSet implements InputSet {
  private Map<String, String> customVariables;

  @Override
  public Type getType() {
    return Type.Manual;
  }
}
