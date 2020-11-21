package software.wings.sm.states;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsSetupContextVariableHolder {
  private Map<String, String> serviceVariables;
  private Map<String, String> safeDisplayServiceVariables;
}
