package software.wings.core.winrm.executors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.data.structure.EmptyPredicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
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
    return format("-e '%s' -u '%s' -s '%s' -env %s -w '%s' -t '%s' -cfile '%s'", hostname, username,
        serverCertValidation, buildEnvironmentStrForCommandLine(), workingDir, timeout, commandFilePath);
  }

  private String buildEnvironmentStrForCommandLine() {
    if (environmentMap == null || EmptyPredicate.isEmpty(environmentMap)) {
      return String.format("%s", Collections.emptyMap());
    }
    return environmentMap.entrySet()
        .stream()
        .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
        .map(this ::quoteAndEscapeQuote)
        .collect(joining(SPACE));
  }

  private String quoteAndEscapeQuote(String val) {
    return String.format("$'%s'", val.replace("'", "\\'"));
  }
}
