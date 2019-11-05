package io.harness.governance.pipeline.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;
import software.wings.beans.HarnessTagLink;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {
  @Nonnull private String key;
  @Nullable private String value;

  public static Tag fromTagLink(HarnessTagLink tagLink) {
    return new Tag(tagLink.getKey(), tagLink.getValue());
  }
}
