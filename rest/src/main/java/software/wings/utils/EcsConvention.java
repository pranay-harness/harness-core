package software.wings.utils;

import java.util.regex.Pattern;

/**
 * Created by rishi on 2/7/17.
 */
public class EcsConvention {
  public static final String DELIMITER = "__";
  private static Pattern wildCharPattern = Pattern.compile("[:.+*/\\\\ &$|\"']");

  public static String getTaskFamily(String appName, String serviceName, String envName) {
    return Misc.normalizeExpression(appName + DELIMITER + serviceName + DELIMITER + envName);
  }

  public static String getServiceName(String family, Integer revision) {
    return family + DELIMITER + revision;
  }

  public static String getServiceNamePrefixFromServiceName(String serviceName) {
    return serviceName.substring(0, serviceName.lastIndexOf(DELIMITER) + DELIMITER.length());
  }

  public static String getContainerName(String imageName) {
    return wildCharPattern.matcher(imageName).replaceAll("_").toLowerCase();
  }

  public static int getRevisionFromServiceName(String name) {
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
}
