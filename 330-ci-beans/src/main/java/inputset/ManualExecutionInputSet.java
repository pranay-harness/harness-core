package inputset;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManualExecutionInputSet implements InputSet {
  private Map<String, String> customVariables;

  @Override
  public Type getType() {
    return Type.Manual;
  }
}
