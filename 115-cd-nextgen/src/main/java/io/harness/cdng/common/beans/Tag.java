package io.harness.cdng.common.beans;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import javax.validation.constraints.NotNull;

@Data
@Builder
public class Tag implements Serializable {
  @NotNull private String key;
  @NotNull private String value;
}
