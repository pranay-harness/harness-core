package software.wings.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.regex.Pattern;

@OwnedBy(CDP)
public class LambdaConvention {
  private static final String DASH = "-";
  private static Pattern wildCharPattern = Pattern.compile("[+*/\\\\ &$|\"']");

  public static String normalizeFunctionName(String functionName) {
    return wildCharPattern.matcher(functionName).replaceAll(DASH);
  }
}
