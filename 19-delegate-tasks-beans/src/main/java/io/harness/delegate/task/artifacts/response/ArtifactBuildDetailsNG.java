package io.harness.delegate.task.artifacts.response;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ArtifactBuildDetailsNG {
  String number;
  String buildUrl;
  Map<String, String> metadata;
  String uiDisplayName;
  Map<String, String> labelsMap;
}
