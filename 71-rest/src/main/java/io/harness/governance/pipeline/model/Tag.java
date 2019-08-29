package io.harness.governance.pipeline.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {
  @Nonnull private String key;
  @Nullable private String value;
}
