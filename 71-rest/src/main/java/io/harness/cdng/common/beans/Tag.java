package io.harness.cdng.common.beans;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class Tag {
  @NotNull private String key;
  @NotNull private String value;
}
