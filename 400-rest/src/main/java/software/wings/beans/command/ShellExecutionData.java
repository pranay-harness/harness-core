package software.wings.beans.command;

import io.harness.shell.CommandExecutionData;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShellExecutionData implements CommandExecutionData {
  private Map<String, String> sweepingOutputEnvVariables;
}
