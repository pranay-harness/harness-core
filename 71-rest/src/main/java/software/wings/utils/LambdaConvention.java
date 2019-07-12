package software.wings.utils;

import java.util.regex.Pattern;

public class LambdaConvention {
  private static final String DASH = "-";
  private static Pattern wildCharPattern = Pattern.compile("[+*/\\\\ &$|\"']");

  public static String normalizeFunctionName(String functionName) {
    return wildCharPattern.matcher(functionName).replaceAll(DASH);
  }
}
