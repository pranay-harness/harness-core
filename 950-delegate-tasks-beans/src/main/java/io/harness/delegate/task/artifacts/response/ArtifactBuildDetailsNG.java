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
