package io.harness.artifacts.beans;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildDetailsInternal {
  String number;
  String buildUrl;
  Map<String, String> metadata;
  String uiDisplayName;
}
