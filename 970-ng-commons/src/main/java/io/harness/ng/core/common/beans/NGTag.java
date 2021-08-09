package io.harness.ng.core.common.beans;

import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NGTagKeys")
public class NGTag implements Serializable {
  @NotNull private String key;
  @NotNull private String value;
}
