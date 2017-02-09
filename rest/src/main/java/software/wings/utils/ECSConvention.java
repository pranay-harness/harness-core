package software.wings.utils;

/**
 * Created by rishi on 2/7/17.
 */
public class ECSConvention {
  private static final String DELIMETER = "__";

  public static String getTaskFamily(String appName, String serviceName, String envName) {
    return appName + DELIMETER + serviceName + DELIMETER + envName;
  }
}
