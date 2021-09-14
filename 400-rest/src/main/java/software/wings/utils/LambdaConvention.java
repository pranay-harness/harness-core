/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import java.util.regex.Pattern;

public class LambdaConvention {
  private static final String DASH = "-";
  private static Pattern wildCharPattern = Pattern.compile("[+*/\\\\ &$|\"']");

  public static String normalizeFunctionName(String functionName) {
    return wildCharPattern.matcher(functionName).replaceAll(DASH);
  }
}
