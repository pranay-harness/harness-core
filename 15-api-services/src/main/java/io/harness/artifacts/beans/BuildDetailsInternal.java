package io.harness.artifacts.beans;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class BuildDetailsInternal {
  String number;
  String buildUrl;
  Map<String, String> metadata;
  String uiDisplayName;
}
