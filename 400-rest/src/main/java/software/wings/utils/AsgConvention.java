/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import io.harness.logging.Misc;

/**
 * Created by anubhaw on 12/24/17.
 */
public class AsgConvention {
  private static final String DELIMITER = "__";

  public static String getAsgNamePrefix(String appName, String serviceName, String envName) {
    return Misc.normalizeExpression(appName + DELIMITER + serviceName + DELIMITER + envName);
  }

  public static String getAsgName(String asgNamePrefix, Integer revision) {
    return asgNamePrefix + DELIMITER + revision;
  }

  public static String getRevisionTagValue(String tagValuePrefix, Integer revision) {
    return tagValuePrefix + DELIMITER + revision;
  }

  public static int getRevisionFromTag(String tagValue) {
    if (tagValue != null) {
      int index = tagValue.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return Integer.parseInt(tagValue.substring(index + DELIMITER.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return 0;
  }
}
