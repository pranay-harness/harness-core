package software.wings.beans.command;

import io.harness.delegate.command.CommandExecutionData;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ShellExecutionData implements CommandExecutionData {
  private Map<String, String> sweepingOutputEnvVariables;
}
