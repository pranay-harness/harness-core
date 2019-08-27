package io.harness.governance.pipeline.model;

import lombok.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Value
public class Tag {
  @Nonnull private String key;
  @Nullable private String value;
  @Nonnull private String uuid;
}
