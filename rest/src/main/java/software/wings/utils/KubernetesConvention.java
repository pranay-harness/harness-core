package software.wings.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brett on 3/8/17
 */
public class KubernetesConvention {
  private static final String DELIMITER = ".";
  private static final String WILD_CHAR_REPLACEMENT = "-";
  private static Pattern wildCharPattern = Pattern.compile("[_+*/\\\\ &$|\"']");

  public static String getReplicationControllerName(String appName, String serviceName, String envName, int revision) {
    return getReplicationControllerNamePrefix(appName, serviceName, envName) + revision;
  }

  public static String getReplicationControllerNamePrefix(String appName, String serviceName, String envName) {
    return normalize(appName + DELIMITER + serviceName + DELIMITER + envName + DELIMITER);
  }

  public static int getRevisionFromControllerName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return Integer.parseInt(name.substring(index + DELIMITER.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return -1;
  }

  private static String normalize(String expression) {
    Matcher matcher = wildCharPattern.matcher(expression);
    return matcher.replaceAll(WILD_CHAR_REPLACEMENT).toLowerCase();
  }
}
