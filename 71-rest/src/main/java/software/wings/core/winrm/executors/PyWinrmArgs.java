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

  /**
   * <ul>
   *   <li>Replaces the single quotes inside the key or value to escaped single quotes and checks if the value ends
   *   with slash, add one more slash so that the single quote at the end won't get esccaped</li>
   *   <li>The regex uses lookbehind to check how many slashes are present before the single quote.
   *   It only replaces if there are even number of slashes which would be considered as non-escaped inside shell.
   *   </li>
   * </ul>
   *
   */
  private String quoteAndEscapeQuote(String val) {
    // Checks if there are even number of slashes before quote. If yes, then add one extra slash to make it odd so
    // that the quote becomes escaped single quote. For example ' become \' and \\' becomes \\\'
    String formattedValue = val.replaceAll("(?<!\\\\)(?:\\\\{2})*'", "\\\\'");
    if (formattedValue.endsWith("\\") && !formattedValue.endsWith("\\\\")) {
      formattedValue += "\\";
    }
    return String.format("$'%s'", formattedValue);
  }
}
