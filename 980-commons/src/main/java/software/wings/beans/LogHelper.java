/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.beans;

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
}
