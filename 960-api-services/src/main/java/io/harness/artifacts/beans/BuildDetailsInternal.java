package io.harness.artifacts.beans;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;

@Value
@Builder
public class BuildDetailsInternal {
  @UtilityClass
  public static final class BuildDetailsInternalMetadataKeys {
    public static final String image = "image";
    public static final String tag = "tag";
  }

  String number;
  String buildUrl;
  Map<String, String> metadata;
  String uiDisplayName;
}
