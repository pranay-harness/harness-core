package software.wings.core.winrm.executors;

import static java.lang.String.format;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class PyWinrmArgs {
  private String username;
  private String hostname;
  private Map<String, String> environmentMap;
  private String workingDir;
  private Integer timeout;
  private boolean serverCertValidation;

  public String getArgs(String commandFilePath) {
    return format("-e %s -u %s -s %s -env '%s' -w '%s' -t %s -cfile %s", hostname, username, serverCertValidation,
        environmentMap, workingDir, timeout, commandFilePath);
  }
}
