package software.wings.sm.states;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class EcsSetupContextVariableHolder {
  private Map<String, String> serviceVariables;
  private Map<String, String> safeDisplayServiceVariables;
}