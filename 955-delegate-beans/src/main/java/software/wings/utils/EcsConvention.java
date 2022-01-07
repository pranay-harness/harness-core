/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.apache.commons.lang3.StringUtils.trim;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.Misc;

import java.util.regex.Pattern;

/**
 * Created by rishi on 2/7/17.
 */
@OwnedBy(CDP)
public class EcsConvention {
  public static final String DELIMITER = "__";
  private static final String VOLUME_PREFIX = "vol_";
  private static final String VOLUME_SUFFIX = "_vol";
  private static Pattern wildCharPattern = Pattern.compile("[:.+*/\\\\ &$|\"']");

  public static String getTaskFamily(String appName, String serviceName, String envName) {
    return Misc.normalizeExpression(appName + DELIMITER + serviceName + DELIMITER + envName);
  }

  public static String getServiceName(String family, Integer revision) {
    return getServiceNamePrefix(family) + revision;
  }

  public static String getServiceNamePrefix(String family) {
    return trim(family) + DELIMITER;
  }

  /**
   * Remove last __NUM part from service.
   * e.g. app__service__env__1, prefix = app__service__env__
   * app__service__env, prefix = app__service__env
   */
  public static String getServiceNamePrefixFromServiceName(String serviceName) {
    if (serviceName.lastIndexOf(DELIMITER) == -1) {
      return serviceName;
    }

    try {
      // If serviceName does not end with __NUM, return entire serviceName
      Integer.parseInt(serviceName.substring(serviceName.lastIndexOf(DELIMITER) + DELIMITER.length()));
    } catch (NumberFormatException e) {
      return serviceName;
    }

    return serviceName.substring(0, serviceName.lastIndexOf(DELIMITER) + DELIMITER.length());
  }

  public static String getContainerName(String imageName) {
    return wildCharPattern.matcher(imageName).replaceAll("_").toLowerCase();
  }

  public static String getVolumeName(String path) {
    return VOLUME_PREFIX + wildCharPattern.matcher(path).replaceAll(DELIMITER).toLowerCase() + VOLUME_SUFFIX;
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
