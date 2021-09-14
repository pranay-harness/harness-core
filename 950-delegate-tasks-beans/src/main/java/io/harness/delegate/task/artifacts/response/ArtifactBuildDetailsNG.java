/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts.response;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactBuildDetailsNG {
  String number;
  String buildUrl;
  Map<String, String> metadata;
  String uiDisplayName;
  Map<String, String> labelsMap;
}
