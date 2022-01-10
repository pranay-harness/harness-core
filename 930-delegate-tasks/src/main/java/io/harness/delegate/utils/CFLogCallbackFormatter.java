/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.pcf.PcfUtils.encodeColor;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDP)
public class CFLogCallbackFormatter {
  private final String APPLICATION_NAME = "APPLICATION-NAME:";
  private final String CURRENT_INSTANCE_COUNT = "CURRENT-INSTANCE-COUNT: ";
  private final String DESIRED_INSTANCE_COUNT = "DESIRED-INSTANCE-COUNT: ";
  private final String NAME = "NAME: ";
  private final String INSTANCE_COUNT = "INSTANCE-COUNT: ";
  private final String ROUTES = "ROUTES: ";

  public String formatAppInstancesState(
      final String appName, Integer currentCountOfInstances, Integer desiredCountOfInstances) {
    return format("%s%s%s", formatSameLineKeyValue(APPLICATION_NAME, encodeColor(appName)),
        formatNewLineKeyValue(CURRENT_INSTANCE_COUNT, currentCountOfInstances),
        formatNewLineKeyValue(DESIRED_INSTANCE_COUNT, desiredCountOfInstances));
  }

  public String formatAppInstancesRoutes(final String appName, Integer countOfInstances, List<String> routes) {
    return format("%s%s%s%n", formatSameLineKeyValue(NAME, encodeColor(appName)),
        formatNewLineKeyValue(INSTANCE_COUNT, countOfInstances), formatNewLineKeyValue(ROUTES, routes));
  }

  public String formatManifestVars(final String appName, Integer countOfInstances, List<String> routes) {
    return format("%s%s%s%n", formatSameLineKeyValue(NAME, encodeColor(appName)),
        formatNewLineKeyValue(INSTANCE_COUNT, countOfInstances), formatNewLineKeyValue(ROUTES, routes));
  }

  public String formatNewLineKeyValue(final String key, final Object value) {
    return format("%n%s %s", key, value);
  }

  public String formatSameLineKeyValue(final String key, final Object value) {
    return format("%s %s", key, value);
  }
}
