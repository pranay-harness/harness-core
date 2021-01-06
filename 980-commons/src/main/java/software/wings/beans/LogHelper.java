package software.wings.beans;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public class LogHelper {
  static final String END_MARK = "#==#";
  static final String NO_FORMATTING = "\033[0m";
  public static final String COMMAND_UNIT_PLACEHOLDER = "-commandUnit:%s";

  static int getBackgroundColorValue(LogColor background) {
    return background.value + 10;
  }

  public static String color(String value, LogColor color) {
    return color(value, color, LogWeight.Normal, LogColor.Black);
  }

  public static String color(String value, LogColor color, LogWeight weight) {
    return color(value, color, weight, LogColor.Black);
  }

  public static String color(String value, LogColor color, LogWeight weight, LogColor background) {
    String format = "\033[" + weight.value + ";" + color.value + "m\033[" + getBackgroundColorValue(background) + "m";
    return format + value.replaceAll(END_MARK, format).replaceAll("\\n", "\n" + format) + END_MARK;
  }

  public static String doneColoring(String value) {
    return value.replaceAll(END_MARK, NO_FORMATTING);
  }

  @Nonnull
  public static String generateLogBaseKey(LinkedHashMap<String, String> logStreamingAbstractions) {
    // Generate base log key that will be used for witing logs to log streaming service
    StringBuilder logBaseKey = new StringBuilder();
    for (Map.Entry<String, String> entry : logStreamingAbstractions.entrySet()) {
      if (logBaseKey.length() != 0) {
        logBaseKey.append("-");
      }
      logBaseKey.append(entry.getKey()).append(":").append(entry.getValue());
    }
    return logBaseKey.toString();
  }
}
