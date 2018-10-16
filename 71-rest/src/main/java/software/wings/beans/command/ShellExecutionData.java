package software.wings.beans.command;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ShellExecutionData extends CommandExecutionData {
  private Map<String, String> sweepingOutputEnvVariables;
}
