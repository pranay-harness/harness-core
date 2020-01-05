package io.harness.windows;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CmdUtils {
  public static String escapeEnvValueSpecialChars(String value) {
    /*
    https://docs.microsoft.com/en-us/windows-server/administration/windows-commands/set_1
    */

    if (!isValidEnvValue(value)) {
      return "";
    }

    value = value.replace("^", "^^");
    value = value.replace("<", "^<");
    value = value.replace(">", "^>");
    value = value.replace("|", "^|");
    value = value.replace("&", "^&");
    value = value.replace("%", "^%");

    return value;
  }

  private static boolean isValidEnvValue(String value) {
    /*
    Rules:
    Should contain even number of '%' chars
    pair of '%' cannot be adjacent - unless they are part of different pairs
    path       - Valid
    %path      - Invalid
    %path%     - Valid
    %pa%th%    - InValid
    %pa%%th%   - Valid
     */

    boolean valid = true;
    int currentIndex = -1;
    int occurrences = 0;
    while (true) {
      int nextIndex = value.indexOf('%', currentIndex + 1);
      if (nextIndex == -1) {
        break;
      }
      occurrences++;
      if (nextIndex == currentIndex + 1) {
        if (occurrences % 2 == 0) {
          valid = false;
          break;
        }
      }
      currentIndex = nextIndex;
    }
    if (occurrences % 2 != 0) {
      valid = false;
    }
    return valid;
  }
}
