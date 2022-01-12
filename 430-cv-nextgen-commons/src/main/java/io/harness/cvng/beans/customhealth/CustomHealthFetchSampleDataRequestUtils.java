/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.beans.customhealth;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;
import io.harness.serializer.JsonUtils;

import java.util.HashMap;
import java.util.Map;
public class CustomHealthFetchSampleDataRequestUtils {
  public static Map<String, Object> getDSLEnvironmentVariables(
      String urlPath, CustomHealthMethod method, String body, TimestampInfo startTime, TimestampInfo endTime) {
    Map<String, Object> commonEnvVariables = new HashMap<>();
    commonEnvVariables.put("method", method);
    commonEnvVariables.put("urlPath", urlPath);
    commonEnvVariables.put("body", isNotEmpty(body) ? JsonUtils.asMap(body) : null);

    commonEnvVariables.put("startTimePlaceholder", startTime.getPlaceholder());
    commonEnvVariables.put("startTimeFormat", startTime.getTimestampFormat().toString());
    if (startTime.getTimestampFormat().equals(TimestampInfo.TimestampFormat.CUSTOM)) {
      commonEnvVariables.put("startTimeCustomFormat", startTime.getCustomTimestampFormat());
    }

    commonEnvVariables.put("endTimePlaceholder", endTime.getPlaceholder());
    commonEnvVariables.put("endTimeFormat", endTime.getTimestampFormat().toString());
    if (endTime.getTimestampFormat().equals(TimestampInfo.TimestampFormat.CUSTOM)) {
      commonEnvVariables.put("endTimeCustomFormat", endTime.getCustomTimestampFormat());
    }
    return commonEnvVariables;
  }
}
