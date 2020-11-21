package io.harness.ng.core.common.beans;

import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NGTag implements Serializable {
  @NotNull private String key;
  @NotNull private String value;
}
