package io.harness.manifest;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomSourceConfig {
  @Nullable private String script;
  private String path;

  @Nullable
  public static CustomSourceConfig cloneFrom(@Nullable CustomSourceConfig sourceConfig) {
    if (sourceConfig == null) {
      return null;
    }

    return CustomSourceConfig.builder().script(sourceConfig.getScript()).path(sourceConfig.getPath()).build();
  }
}
